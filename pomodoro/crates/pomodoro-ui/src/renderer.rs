use image::{DynamicImage, ImageReader, RgbaImage};
use std::collections::HashMap;
use std::path::Path;
use tracing::{debug, warn};

pub struct BreakUiRenderer {
    cached_image: Option<DynamicImage>,
    resized_cache: HashMap<(u32, u32), RgbaImage>,
}

impl Default for BreakUiRenderer {
    fn default() -> Self {
        Self::new()
    }
}

impl BreakUiRenderer {
    pub fn new() -> Self {
        let mut renderer = Self {
            cached_image: None,
            resized_cache: HashMap::new(),
        };
        renderer.load_asset();
        renderer
    }

    fn load_asset(&mut self) {
        let candidate_paths = [
            "/home/dev/SE/pomodoro/assets/break_motivation.png",
            "/home/dev/SE/assets/break_motivation.png",
            "assets/break_motivation.png",
            "../assets/break_motivation.png",
        ];

        for path_str in &candidate_paths {
            let path = Path::new(path_str);
            if path.exists() {
                if let Ok(reader) = ImageReader::open(path) {
                    if let Ok(img) = reader.decode() {
                        debug!("Loaded motivational break image from {}", path_str);
                        self.cached_image = Some(img);
                        return;
                    }
                }
            }
        }

        warn!("Motivational image asset not found in candidate paths");
    }

    /// Renders the complete break visual frame onto an RGBA/BGRA 32-bit pixel buffer.
    pub fn render_break_frame(
        &mut self,
        buffer: &mut [u32],
        width: u32,
        height: u32,
        time_remaining_str: &str,
    ) {
        if width == 0 || height == 0 || buffer.len() < (width * height) as usize {
            return;
        }

        // 1. Fill modern slate background
        buffer[..((width * height) as usize)].fill(0xFF0F111A);

        // 2. Render centered image if available (with cached resized RGBA)
        if let Some(ref img) = self.cached_image {
            let img_w = img.width();
            let img_h = img.height();

            let max_target_h = (height as f32 * 0.55).min(680.0);
            let max_target_w = (width as f32 * 0.85).min(1200.0);

            let scale = (max_target_w / img_w as f32).min(max_target_h / img_h as f32);
            let target_w = (img_w as f32 * scale) as u32;
            let target_h = (img_h as f32 * scale) as u32;

            let start_x = (width.saturating_sub(target_w)) / 2;
            let start_y = (height.saturating_sub(target_h)) / 2;

            // Cache resized image per target dimensions to avoid expensive resamples every frame
            let raw_rgba = self
                .resized_cache
                .entry((target_w, target_h))
                .or_insert_with(|| {
                    img.resize(target_w, target_h, image::imageops::FilterType::Triangle)
                        .to_rgba8()
                });

            for y in 0..target_h {
                let dst_y = start_y + y;
                if dst_y >= height {
                    break;
                }
                for x in 0..target_w {
                    let dst_x = start_x + x;
                    if dst_x >= width {
                        break;
                    }
                    let pixel = raw_rgba.get_pixel(x, y);
                    let r = pixel[0];
                    let g = pixel[1];
                    let b = pixel[2];
                    let a = pixel[3];

                    let idx = (dst_y * width + dst_x) as usize;
                    if idx < buffer.len() {
                        buffer[idx] = ((a as u32) << 24)
                            | ((r as u32) << 16)
                            | ((g as u32) << 8)
                            | (b as u32);
                    }
                }
            }
        }

        // 3. Top Banner
        let top_banner_h = 100u32;
        let top_end = ((top_banner_h.min(height)) * width) as usize;
        if top_end <= buffer.len() {
            buffer[..top_end].fill(0xF0161926);
        }

        // Draw Title "TAKE A BREAK"
        draw_text_bitmap(
            buffer,
            width,
            height,
            "TAKE A BREAK",
            (width.saturating_sub(12 * 8 * 3)) / 2,
            16,
            3,
            0xFFF0F2F5,
        );

        // Draw Countdown Timer
        let timer_len = time_remaining_str.len() as u32;
        let timer_x = (width.saturating_sub(timer_len * 8 * 4)) / 2;
        draw_text_bitmap(
            buffer,
            width,
            height,
            time_remaining_str,
            timer_x,
            55,
            4,
            0xFF4FD1C5, // Modern teal accent
        );

        // 4. Bottom Banner: "Take a deep breath, You still have a long way !"
        let bottom_banner_h = 90u32;
        let start_bottom_y = height.saturating_sub(bottom_banner_h);
        let start_bot_idx = (start_bottom_y * width) as usize;
        let end_bot_idx = (height * width) as usize;
        if start_bot_idx < end_bot_idx && end_bot_idx <= buffer.len() {
            buffer[start_bot_idx..end_bot_idx].fill(0xF0141622);
        }

        let quote = "Take a deep breath, You still have a long way !";
        let quote_len = quote.len() as u32;
        let quote_scale = if width > 1400 { 3 } else { 2 };
        let quote_x = (width.saturating_sub(quote_len * 8 * quote_scale)) / 2;
        let quote_y = start_bottom_y + 30;

        draw_text_bitmap(
            buffer,
            width,
            height,
            quote,
            quote_x,
            quote_y,
            quote_scale,
            0xFFE2E8F0,
        );
    }

    /// Renders compact floating working phase indicator
    pub fn render_work_frame(
        &self,
        buffer: &mut [u32],
        width: u32,
        height: u32,
        time_remaining_str: &str,
    ) {
        if width == 0 || height == 0 || buffer.len() < (width * height) as usize {
            return;
        }

        // 1. Fill inner dark slate background in one fast vectorized operation
        buffer[..((width * height) as usize)].fill(0xFF1C2230);

        // 2. Fill top 2 border rows
        let top_rows = (2 * width).min(width * height) as usize;
        buffer[..top_rows].fill(0xFF38BDF8);

        // 3. Fill bottom 2 border rows
        let start_bot = (height.saturating_sub(2) * width) as usize;
        let end_bot = (width * height) as usize;
        if start_bot < end_bot && end_bot <= buffer.len() {
            buffer[start_bot..end_bot].fill(0xFF38BDF8);
        }

        // 4. Fill left & right 2px border columns
        for y in 0..height {
            let row_offset = (y * width) as usize;
            if row_offset + (width as usize) <= buffer.len() {
                buffer[row_offset] = 0xFF38BDF8;
                if width > 1 {
                    buffer[row_offset + 1] = 0xFF38BDF8;
                }
                if width >= 2 {
                    buffer[row_offset + (width as usize) - 2] = 0xFF38BDF8;
                    buffer[row_offset + (width as usize) - 1] = 0xFF38BDF8;
                }
            }
        }

        // Draw Header text "FOCUS (25m)"
        draw_text_bitmap(
            buffer,
            width,
            height,
            "FOCUS (25m)",
            (width.saturating_sub(11 * 8)) / 2,
            12,
            1,
            0xFF94A3B8,
        );

        // Draw Big Crisp Countdown Timer
        let timer_len = time_remaining_str.len() as u32;
        let scale = if height >= 80 { 4 } else { 3 };
        let timer_x = (width.saturating_sub(timer_len * 8 * scale)) / 2;
        let timer_y = if height >= 80 { 32 } else { 25 };

        draw_text_bitmap(
            buffer,
            width,
            height,
            time_remaining_str,
            timer_x,
            timer_y,
            scale,
            0xFF38BDF8, // Bright sky blue
        );

        if height >= 85 {
            // Draw tiny subtext
            draw_text_bitmap(
                buffer,
                width,
                height,
                "5m break auto-enforces",
                (width.saturating_sub(22 * 8)) / 2,
                height.saturating_sub(18),
                1,
                0xFF64748B,
            );
        }
    }
}

// Built-in 8x8 font bitmap table for ASCII 32..=126
fn get_glyph_bitmap(ch: char) -> [u8; 8] {
    match ch {
        ' ' => [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00],
        '!' => [0x18, 0x18, 0x18, 0x18, 0x18, 0x00, 0x18, 0x00],
        '"' => [0x66, 0x66, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00],
        '#' => [0x6c, 0x6c, 0xfe, 0x6c, 0xfe, 0x6c, 0x6c, 0x00],
        '$' => [0x18, 0x7e, 0xc0, 0x7c, 0x06, 0xfc, 0x18, 0x00],
        '%' => [0x00, 0xc6, 0xcc, 0x18, 0x30, 0x66, 0xc6, 0x00],
        '&' => [0x38, 0x6c, 0x38, 0x76, 0xdc, 0xcc, 0x76, 0x00],
        '\'' => [0x18, 0x18, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00],
        '(' => [0x0c, 0x18, 0x30, 0x30, 0x30, 0x18, 0x0c, 0x00],
        ')' => [0x30, 0x18, 0x0c, 0x0c, 0x0c, 0x18, 0x30, 0x00],
        '*' => [0x00, 0x66, 0x3c, 0xff, 0x3c, 0x66, 0x00, 0x00],
        '+' => [0x00, 0x18, 0x18, 0x7e, 0x18, 0x18, 0x00, 0x00],
        ',' => [0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x18, 0x30],
        '-' => [0x00, 0x00, 0x00, 0x7e, 0x00, 0x00, 0x00, 0x00],
        '.' => [0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x18, 0x00],
        '/' => [0x06, 0x0c, 0x18, 0x30, 0x60, 0xc0, 0x80, 0x00],
        '0' => [0x3c, 0x66, 0x6e, 0x76, 0x66, 0x66, 0x3c, 0x00],
        '1' => [0x18, 0x38, 0x18, 0x18, 0x18, 0x18, 0x7e, 0x00],
        '2' => [0x7c, 0xc6, 0x0e, 0x3c, 0x78, 0xe0, 0xfe, 0x00],
        '3' => [0x7e, 0x0c, 0x18, 0x3c, 0x06, 0xc6, 0x7c, 0x00],
        '4' => [0x1c, 0x3c, 0x6c, 0xcc, 0xfe, 0x0c, 0x1e, 0x00],
        '5' => [0xfe, 0xc0, 0xfc, 0x06, 0x06, 0xc6, 0x7c, 0x00],
        '6' => [0x38, 0x60, 0xc0, 0xfc, 0xc6, 0xc6, 0x7c, 0x00],
        '7' => [0xfe, 0xc6, 0x0c, 0x18, 0x30, 0x30, 0x30, 0x00],
        '8' => [0x7c, 0xc6, 0xc6, 0x7c, 0xc6, 0xc6, 0x7c, 0x00],
        '9' => [0x7c, 0xc6, 0xc6, 0x7e, 0x06, 0x0c, 0x78, 0x00],
        ':' => [0x00, 0x18, 0x18, 0x00, 0x18, 0x18, 0x00, 0x00],
        ';' => [0x00, 0x18, 0x18, 0x00, 0x18, 0x18, 0x30, 0x00],
        '<' => [0x06, 0x0c, 0x18, 0x30, 0x18, 0x0c, 0x06, 0x00],
        '=' => [0x00, 0x7e, 0x00, 0x7e, 0x00, 0x00, 0x00, 0x00],
        '>' => [0x60, 0x30, 0x18, 0x0c, 0x18, 0x30, 0x60, 0x00],
        '?' => [0x7c, 0xc6, 0x0c, 0x18, 0x18, 0x00, 0x18, 0x00],
        '@' => [0x7c, 0xc6, 0xde, 0xde, 0xdc, 0xc0, 0x7c, 0x00],
        'A' | 'a' => [0x3c, 0x66, 0xc3, 0xff, 0xc3, 0xc3, 0xc3, 0x00],
        'B' | 'b' => [0xfc, 0x66, 0x66, 0x7c, 0x66, 0x66, 0xfc, 0x00],
        'C' | 'c' => [0x3c, 0x66, 0xc0, 0xc0, 0xc0, 0x66, 0x3c, 0x00],
        'D' | 'd' => [0xf8, 0x6c, 0x66, 0x66, 0x66, 0x6c, 0xf8, 0x00],
        'E' | 'e' => [0xfe, 0x62, 0x68, 0x78, 0x68, 0x62, 0xfe, 0x00],
        'F' | 'f' => [0xfe, 0x62, 0x68, 0x78, 0x68, 0x60, 0xf0, 0x00],
        'G' | 'g' => [0x3c, 0x66, 0xc0, 0xcf, 0xc6, 0x66, 0x3e, 0x00],
        'H' | 'h' => [0xc3, 0xc3, 0xc3, 0xff, 0xc3, 0xc3, 0xc3, 0x00],
        'I' | 'i' => [0x7e, 0x18, 0x18, 0x18, 0x18, 0x18, 0x7e, 0x00],
        'J' | 'j' => [0x1e, 0x06, 0x06, 0x06, 0xc6, 0xc6, 0x7c, 0x00],
        'K' | 'k' => [0xc6, 0xcc, 0xd8, 0xf0, 0xd8, 0xcc, 0xc6, 0x00],
        'L' | 'l' => [0xf0, 0x60, 0x60, 0x60, 0x62, 0x66, 0xfe, 0x00],
        'M' | 'm' => [0xc3, 0xe7, 0xff, 0xdb, 0xc3, 0xc3, 0xc3, 0x00],
        'N' | 'n' => [0xc3, 0xe3, 0xf3, 0xdb, 0xcf, 0xc7, 0xc3, 0x00],
        'O' | 'o' => [0x7c, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0x7c, 0x00],
        'P' | 'p' => [0xfc, 0x66, 0x66, 0x7c, 0x60, 0x60, 0xf0, 0x00],
        'Q' | 'q' => [0x7c, 0xc6, 0xc6, 0xc6, 0xd6, 0xde, 0x7c, 0x06],
        'R' | 'r' => [0xfc, 0x66, 0x66, 0x7c, 0x6c, 0x66, 0xe3, 0x00],
        'S' | 's' => [0x7c, 0xc6, 0x60, 0x3c, 0x06, 0xc6, 0x7c, 0x00],
        'T' | 't' => [0x7e, 0x5a, 0x18, 0x18, 0x18, 0x18, 0x3c, 0x00],
        'U' | 'u' => [0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x00],
        'V' | 'v' => [0xc3, 0xc3, 0xc3, 0x66, 0x66, 0x3c, 0x18, 0x00],
        'W' | 'w' => [0xc3, 0xc3, 0xc3, 0xdb, 0xff, 0xe7, 0xc3, 0x00],
        'X' | 'x' => [0xc3, 0x66, 0x3c, 0x18, 0x3c, 0x66, 0xc3, 0x00],
        'Y' | 'y' => [0xc3, 0xc3, 0x66, 0x3c, 0x18, 0x18, 0x3c, 0x00],
        'Z' | 'z' => [0xff, 0x86, 0x0c, 0x18, 0x30, 0x61, 0xff, 0x00],
        _ => [0xff, 0x81, 0xbd, 0xbd, 0xbd, 0xbd, 0x81, 0xff],
    }
}

#[allow(clippy::too_many_arguments)]
fn draw_text_bitmap(
    buffer: &mut [u32],
    screen_w: u32,
    screen_h: u32,
    text: &str,
    start_x: u32,
    start_y: u32,
    scale: u32,
    color: u32,
) {
    let mut cur_x = start_x;
    for ch in text.chars() {
        let bitmap = get_glyph_bitmap(ch);
        for (row_idx, row_byte) in bitmap.iter().enumerate() {
            for col_idx in 0..8 {
                if (row_byte & (0x80 >> col_idx)) != 0 {
                    for sy in 0..scale {
                        for sx in 0..scale {
                            let px = cur_x + (col_idx as u32 * scale) + sx;
                            let py = start_y + (row_idx as u32 * scale) + sy;
                            if px < screen_w && py < screen_h {
                                let idx = (py * screen_w + px) as usize;
                                if idx < buffer.len() {
                                    buffer[idx] = color;
                                }
                            }
                        }
                    }
                }
            }
        }
        cur_x += 8 * scale;
    }
}
