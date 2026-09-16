use serde::{Deserialize, Serialize};
use std::fmt;
use std::ops::{Add, Sub};
use std::time::Duration;

/// Monotonic timestamp representing nanoseconds since an arbitrary boot/epoch point.
/// Guaranteed to be strictly non-decreasing and immune to wall-clock / NTP / DST changes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct MonotonicTime {
    nanos: u64,
}

impl MonotonicTime {
    pub const ZERO: Self = Self { nanos: 0 };

    pub const fn from_nanos(nanos: u64) -> Self {
        Self { nanos }
    }

    pub const fn from_secs(secs: u64) -> Self {
        Self {
            nanos: secs.saturating_mul(1_000_000_000),
        }
    }

    pub const fn as_nanos(&self) -> u64 {
        self.nanos
    }

    pub const fn as_secs(&self) -> u64 {
        self.nanos / 1_000_000_000
    }

    pub const fn as_millis(&self) -> u64 {
        self.nanos / 1_000_000
    }

    /// Calculate remaining duration until deadline from current time.
    /// If current time >= deadline, returns Duration::ZERO.
    pub fn remaining_until(&self, deadline: Self) -> Duration {
        if *self >= deadline {
            Duration::ZERO
        } else {
            Duration::from_nanos(deadline.nanos - self.nanos)
        }
    }

    /// Check if deadline is reached or exceeded.
    pub fn is_expired(&self, deadline: Self) -> bool {
        *self >= deadline
    }

    /// Format remaining duration as MM:SS (or HH:MM:SS if >= 1 hour).
    pub fn format_remaining(remaining: Duration) -> String {
        let total_secs = remaining.as_secs();
        let hours = total_secs / 3600;
        let mins = (total_secs % 3600) / 60;
        let secs = total_secs % 60;

        if hours > 0 {
            format!("{:02}:{:02}:{:02}", hours, mins, secs)
        } else {
            format!("{:02}:{:02}", mins, secs)
        }
    }
}

impl fmt::Display for MonotonicTime {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let total_secs = self.as_secs();
        let frac_millis = (self.nanos % 1_000_000_000) / 1_000_000;
        write!(f, "{}.{:03}s", total_secs, frac_millis)
    }
}

impl Add<Duration> for MonotonicTime {
    type Output = Self;

    fn add(self, rhs: Duration) -> Self::Output {
        let dur_nanos = rhs.as_nanos().min(u64::MAX as u128) as u64;
        Self {
            nanos: self.nanos.saturating_add(dur_nanos),
        }
    }
}

impl Sub<Duration> for MonotonicTime {
    type Output = Self;

    fn sub(self, rhs: Duration) -> Self::Output {
        let dur_nanos = rhs.as_nanos().min(u64::MAX as u128) as u64;
        Self {
            nanos: self.nanos.saturating_sub(dur_nanos),
        }
    }
}

impl Sub<MonotonicTime> for MonotonicTime {
    type Output = Duration;

    fn sub(self, rhs: MonotonicTime) -> Self::Output {
        if self.nanos >= rhs.nanos {
            Duration::from_nanos(self.nanos - rhs.nanos)
        } else {
            Duration::ZERO
        }
    }
}

/// Abstract Clock trait for time queries, enabling deterministic testing.
pub trait Clock: Send + Sync {
    fn now(&self) -> MonotonicTime;
}

/// Production Clock implementation using Linux CLOCK_BOOTTIME.
/// CLOCK_BOOTTIME is identical to CLOCK_MONOTONIC but includes any time that the system is suspended.
#[derive(Debug, Default, Clone, Copy)]
pub struct SystemBootClock;

impl SystemBootClock {
    pub fn new() -> Self {
        Self
    }
}

impl Clock for SystemBootClock {
    fn now(&self) -> MonotonicTime {
        let mut ts = libc::timespec {
            tv_sec: 0,
            tv_nsec: 0,
        };
        // Use CLOCK_BOOTTIME (value 7 in Linux) to include sleep/suspend time
        let res = unsafe { libc::clock_gettime(libc::CLOCK_BOOTTIME, &mut ts) };
        if res == 0 && ts.tv_sec >= 0 && ts.tv_nsec >= 0 {
            let nanos = (ts.tv_sec as u64)
                .saturating_mul(1_000_000_000)
                .saturating_add(ts.tv_nsec as u64);
            MonotonicTime::from_nanos(nanos)
        } else {
            // Fallback to std::time::Instant in non-Linux or fallback scenario
            static START: std::sync::OnceLock<std::time::Instant> = std::sync::OnceLock::new();
            let start = START.get_or_init(std::time::Instant::now);
            let elapsed = start.elapsed();
            MonotonicTime::from_nanos(elapsed.as_nanos().min(u64::MAX as u128) as u64)
        }
    }
}

/// Mock Clock for deterministic property and unit tests.
#[derive(Debug, Clone)]
pub struct MockClock {
    current: std::sync::Arc<std::sync::atomic::AtomicU64>,
}

impl MockClock {
    pub fn new(initial_nanos: u64) -> Self {
        Self {
            current: std::sync::Arc::new(std::sync::atomic::AtomicU64::new(initial_nanos)),
        }
    }

    pub fn set(&self, time: MonotonicTime) {
        self.current
            .store(time.as_nanos(), std::sync::atomic::Ordering::SeqCst);
    }

    pub fn advance(&self, duration: Duration) {
        let nanos = duration.as_nanos().min(u64::MAX as u128) as u64;
        self.current
            .fetch_add(nanos, std::sync::atomic::Ordering::SeqCst);
    }
}

impl Clock for MockClock {
    fn now(&self) -> MonotonicTime {
        MonotonicTime::from_nanos(self.current.load(std::sync::atomic::Ordering::SeqCst))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_monotonic_time_math() {
        let t0 = MonotonicTime::from_secs(10);
        let t1 = t0 + Duration::from_secs(5);
        assert_eq!(t1.as_secs(), 15);
        assert_eq!(t1 - t0, Duration::from_secs(5));
        assert_eq!(t0.remaining_until(t1), Duration::from_secs(5));
        assert_eq!(t1.remaining_until(t0), Duration::ZERO);
        assert!(t1.is_expired(t0));
        assert!(!t0.is_expired(t1));
    }

    #[test]
    fn test_format_remaining() {
        assert_eq!(
            MonotonicTime::format_remaining(Duration::from_secs(1500)),
            "25:00"
        );
        assert_eq!(
            MonotonicTime::format_remaining(Duration::from_secs(300)),
            "05:00"
        );
        assert_eq!(
            MonotonicTime::format_remaining(Duration::from_secs(45)),
            "00:45"
        );
        assert_eq!(
            MonotonicTime::format_remaining(Duration::from_secs(3665)),
            "01:01:05"
        );
    }

    #[test]
    fn test_mock_clock() {
        let clock = MockClock::new(1_000_000_000);
        assert_eq!(clock.now().as_secs(), 1);
        clock.advance(Duration::from_secs(10));
        assert_eq!(clock.now().as_secs(), 11);
    }

    #[test]
    fn test_system_clock_advances() {
        let clock = SystemBootClock::new();
        let t1 = clock.now();
        std::thread::sleep(Duration::from_millis(10));
        let t2 = clock.now();
        assert!(t2 >= t1);
    }
}
