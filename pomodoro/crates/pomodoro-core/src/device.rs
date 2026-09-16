use serde::{Deserialize, Serialize};
use std::path::PathBuf;

// Linux input event definitions from <linux/input-event-codes.h> & <linux/input.h>
pub const EV_SYN: usize = 0x00;
pub const EV_KEY: usize = 0x01;
pub const EV_REL: usize = 0x02;
pub const EV_ABS: usize = 0x03;
pub const EV_MSC: usize = 0x04;
pub const EV_SW: usize = 0x05;
pub const EV_LED: usize = 0x11;
pub const EV_SND: usize = 0x12;
pub const EV_REP: usize = 0x14;
pub const EV_FF: usize = 0x15;
pub const EV_PWR: usize = 0x16;
pub const EV_FF_STATUS: usize = 0x17;
pub const EV_MAX: usize = 0x1f;

pub const KEY_ESC: usize = 1;
pub const KEY_1: usize = 2;
pub const KEY_Q: usize = 16;
pub const KEY_A: usize = 30;
pub const KEY_Z: usize = 44;
pub const KEY_SPACE: usize = 57;
pub const KEY_ENTER: usize = 28;

pub const BTN_MISC: usize = 0x100;
pub const BTN_MOUSE: usize = 0x110;
pub const BTN_LEFT: usize = 0x110;
pub const BTN_RIGHT: usize = 0x111;
pub const BTN_MIDDLE: usize = 0x112;
pub const BTN_SIDE: usize = 0x113;
pub const BTN_EXTRA: usize = 0x114;

pub const BTN_DIGI: usize = 0x140;
pub const BTN_TOOL_PEN: usize = 0x140;
pub const BTN_TOOL_RUBBER: usize = 0x141;
pub const BTN_TOOL_BRUSH: usize = 0x142;
pub const BTN_TOOL_PENCIL: usize = 0x143;
pub const BTN_TOOL_AIRBRUSH: usize = 0x144;
pub const BTN_TOOL_FINGER: usize = 0x145;
pub const BTN_TOOL_MOUSE: usize = 0x146;
pub const BTN_TOOL_LENS: usize = 0x147;
pub const BTN_TOUCH: usize = 0x14a;
pub const BTN_STYLUS: usize = 0x14b;
pub const BTN_STYLUS2: usize = 0x14c;
pub const BTN_TOOL_DOUBLETAP: usize = 0x14d;
pub const BTN_TOOL_TRIPLETAP: usize = 0x14e;
pub const BTN_TOOL_QUADTAP: usize = 0x14f;

pub const REL_X: usize = 0x00;
pub const REL_Y: usize = 0x01;
pub const REL_WHEEL: usize = 0x08;
pub const REL_HWHEEL: usize = 0x06;

pub const ABS_X: usize = 0x00;
pub const ABS_Y: usize = 0x01;
pub const ABS_PRESSURE: usize = 0x18;
pub const ABS_MT_SLOT: usize = 0x2f;
pub const ABS_MT_TOUCH_MAJOR: usize = 0x30;
pub const ABS_MT_POSITION_X: usize = 0x35;
pub const ABS_MT_POSITION_Y: usize = 0x36;
pub const ABS_MT_TRACKING_ID: usize = 0x39;

pub const INPUT_PROP_POINTER: usize = 0x00;
pub const INPUT_PROP_DIRECT: usize = 0x01;
pub const INPUT_PROP_BUTTONPAD: usize = 0x02;
pub const INPUT_PROP_SEMI_MT: usize = 0x03;
pub const INPUT_PROP_TOPBUTTONPAD: usize = 0x04;
pub const INPUT_PROP_POINTING_STICK: usize = 0x05;
pub const INPUT_PROP_ACCELEROMETER: usize = 0x06;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum DeviceClass {
    Keyboard,
    Mouse,
    Touchpad,
    PointingStick,
    Tablet,
    Touchscreen,
    Ignored,
}

impl DeviceClass {
    pub fn is_enforcement_target(&self) -> bool {
        matches!(
            self,
            DeviceClass::Keyboard
                | DeviceClass::Mouse
                | DeviceClass::Touchpad
                | DeviceClass::PointingStick
                | DeviceClass::Tablet
                | DeviceClass::Touchscreen
        )
    }

    pub fn display_name(&self) -> &'static str {
        match self {
            DeviceClass::Keyboard => "Keyboard",
            DeviceClass::Mouse => "Mouse",
            DeviceClass::Touchpad => "Touchpad",
            DeviceClass::PointingStick => "Pointing Stick / TrackPoint",
            DeviceClass::Tablet => "Graphics Tablet",
            DeviceClass::Touchscreen => "Touchscreen",
            DeviceClass::Ignored => "Non-Target Device",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct DeviceInfo {
    pub path: PathBuf,
    pub name: String,
    pub phys: String,
    pub uniq: String,
    pub bustype: u16,
    pub vendor: u16,
    pub product: u16,
    pub version: u16,
    pub class: DeviceClass,
}

#[inline]
pub fn test_bit(bit: usize, bytes: &[u8]) -> bool {
    let byte_idx = bit / 8;
    let bit_idx = bit % 8;
    if byte_idx < bytes.len() {
        (bytes[byte_idx] & (1 << bit_idx)) != 0
    } else {
        false
    }
}

/// Classifies a Linux input device based on its capability bitmasks, properties, and name.
pub fn classify_device(
    name: &str,
    ev_bits: &[u8],
    key_bits: &[u8],
    rel_bits: &[u8],
    abs_bits: &[u8],
    prop_bits: &[u8],
) -> DeviceClass {
    let name_lower = name.to_lowercase();

    // 1. Explicitly ignore known non-human-interaction devices
    if name_lower.contains("power button")
        || name_lower.contains("sleep button")
        || name_lower.contains("lid switch")
        || name_lower.contains("video bus")
        || name_lower.contains("headphone")
        || name_lower.contains("hd-audio")
        || name_lower.contains("hdmi")
        || name_lower.contains("avrcp")
        || name_lower.contains("rfkill")
        || name_lower.contains("camera")
    {
        return DeviceClass::Ignored;
    }

    let has_keys = test_bit(EV_KEY, ev_bits);
    let has_rel = test_bit(EV_REL, ev_bits);
    let has_abs = test_bit(EV_ABS, ev_bits);

    let has_btn_left = has_keys && test_bit(BTN_LEFT, key_bits);
    let has_btn_touch = has_keys && test_bit(BTN_TOUCH, key_bits);
    let has_btn_tool_finger = has_keys && test_bit(BTN_TOOL_FINGER, key_bits);
    let has_btn_tool_pen = has_keys && test_bit(BTN_TOOL_PEN, key_bits);
    let has_buttonpad = test_bit(INPUT_PROP_BUTTONPAD, prop_bits);
    let has_pointing_stick = test_bit(INPUT_PROP_POINTING_STICK, prop_bits);
    let has_direct = test_bit(INPUT_PROP_DIRECT, prop_bits);
    let has_pointer_prop = test_bit(INPUT_PROP_POINTER, prop_bits);

    let has_rel_x = has_rel && test_bit(REL_X, rel_bits);
    let has_rel_y = has_rel && test_bit(REL_Y, rel_bits);

    let has_abs_x = has_abs && test_bit(ABS_X, abs_bits);
    let has_abs_y = has_abs && test_bit(ABS_Y, abs_bits);
    let has_mt = has_abs && test_bit(ABS_MT_POSITION_X, abs_bits);

    // 2. Touchpad classification
    if (has_buttonpad
        || has_btn_tool_finger
        || (has_btn_touch && !has_direct && (has_abs_x || has_abs_y)))
        || name_lower.contains("touchpad")
        || name_lower.contains("trackpad")
    {
        return DeviceClass::Touchpad;
    }

    // 3. PointingStick / TrackPoint
    if has_pointing_stick
        || name_lower.contains("trackpoint")
        || name_lower.contains("pointing stick")
        || name_lower.contains("dualpoint stick")
    {
        return DeviceClass::PointingStick;
    }

    // 4. Touchscreen
    if (has_direct && has_btn_touch && (has_abs_x || has_abs_y || has_mt))
        || name_lower.contains("touchscreen")
    {
        return DeviceClass::Touchscreen;
    }

    // 5. Tablet / Stylus
    if has_btn_tool_pen || name_lower.contains("tablet") || name_lower.contains("stylus") {
        return DeviceClass::Tablet;
    }

    // 6. Mouse
    if (has_btn_left && (has_rel_x || has_rel_y))
        || (has_rel_x && has_rel_y && has_keys)
        || (has_pointer_prop && has_btn_left)
        || name_lower.contains("mouse")
    {
        return DeviceClass::Mouse;
    }

    // 7. Keyboard
    let mut alpha_keys = 0;
    for k in KEY_Q..=KEY_ENTER {
        if test_bit(k, key_bits) {
            alpha_keys += 1;
        }
    }
    for k in KEY_A..=KEY_Z {
        if test_bit(k, key_bits) {
            alpha_keys += 1;
        }
    }

    if alpha_keys >= 8 || name_lower.contains("keyboard") || name_lower.contains("at translated") {
        return DeviceClass::Keyboard;
    }

    DeviceClass::Ignored
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_classify_laptop_keyboard() {
        let mut ev_bits = [0u8; 8];
        ev_bits[0] |= 1 << EV_KEY;
        let mut key_bits = [0u8; 64];
        for k in KEY_A..=KEY_Z {
            let b = k / 8;
            let bit = k % 8;
            key_bits[b] |= 1 << bit;
        }
        let cls = classify_device(
            "AT Translated Set 2 keyboard",
            &ev_bits,
            &key_bits,
            &[],
            &[],
            &[],
        );
        assert_eq!(cls, DeviceClass::Keyboard);
        assert!(cls.is_enforcement_target());
    }

    #[test]
    fn test_classify_touchpad() {
        let mut ev_bits = [0u8; 8];
        ev_bits[0] |= 1 << EV_KEY;
        ev_bits[0] |= 1 << EV_ABS;
        let mut key_bits = [0u8; 64];
        key_bits[BTN_TOUCH / 8] |= 1 << (BTN_TOUCH % 8);
        key_bits[BTN_TOOL_FINGER / 8] |= 1 << (BTN_TOOL_FINGER % 8);
        let mut abs_bits = [0u8; 64];
        abs_bits[ABS_X / 8] |= 1 << (ABS_X % 8);
        abs_bits[ABS_MT_POSITION_X / 8] |= 1 << (ABS_MT_POSITION_X % 8);

        let cls = classify_device(
            "DLL0B48:01 04F3:31E2 Touchpad",
            &ev_bits,
            &key_bits,
            &[],
            &abs_bits,
            &[],
        );
        assert_eq!(cls, DeviceClass::Touchpad);
        assert!(cls.is_enforcement_target());
    }

    #[test]
    fn test_classify_wireless_mouse() {
        let mut ev_bits = [0u8; 8];
        ev_bits[0] |= 1 << EV_KEY;
        ev_bits[0] |= 1 << EV_REL;
        let mut key_bits = [0u8; 64];
        key_bits[BTN_LEFT / 8] |= 1 << (BTN_LEFT % 8);
        key_bits[BTN_RIGHT / 8] |= 1 << (BTN_RIGHT % 8);
        let mut rel_bits = [0u8; 8];
        rel_bits[REL_X / 8] |= 1 << (REL_X % 8);
        rel_bits[REL_Y / 8] |= 1 << (REL_Y % 8);

        let cls = classify_device(
            "LITEON Dell Wireless Device Mouse",
            &ev_bits,
            &key_bits,
            &rel_bits,
            &[],
            &[],
        );
        assert_eq!(cls, DeviceClass::Mouse);
        assert!(cls.is_enforcement_target());
    }

    #[test]
    fn test_classify_ignored_power_button() {
        let mut ev_bits = [0u8; 8];
        ev_bits[0] |= 1 << EV_KEY;
        let cls = classify_device("Power Button", &ev_bits, &[], &[], &[], &[]);
        assert_eq!(cls, DeviceClass::Ignored);
        assert!(!cls.is_enforcement_target());
    }
}
