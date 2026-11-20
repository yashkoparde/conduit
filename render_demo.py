import math
import subprocess
import numpy as np
from PIL import Image, ImageDraw, ImageFont

# ----------------------------------------------------
# CONFIG & 3-COLOR PALETTE
# ----------------------------------------------------
WIDTH = 1280
HEIGHT = 720
FPS = 30
OUTPUT_FILE = "demo_slides.mp4"

C_BG = (13, 17, 23)        # #0D1117 (Dark Charcoal / Canvas)
C_LIGHT = (240, 246, 252)  # #F0F6FC (Off-White / Structures)
C_ACCENT = (255, 107, 74)  # #FF6B4A (Vibrant Coral / Energy Highlight)

TOTAL_FRAMES = 330  # 11 seconds total

# Fixed random seed for reproducible positions
np.random.seed(42)
NUM_DOTS = 40
SCATTER_X = np.random.uniform(150, WIDTH - 150, NUM_DOTS)
SCATTER_Y = np.random.uniform(120, HEIGHT - 120, NUM_DOTS)

# Pre-generate chaotic connections
CHAOS_PAIRS = [(i, (i * 7 + 3) % NUM_DOTS) for i in range(25)]

try:
    FONT_TAG = ImageFont.truetype("arial.ttf", 22)
    FONT_HERO = ImageFont.truetype("arialbd.ttf", 68)
except Exception:
    FONT_TAG = ImageFont.load_default()
    FONT_HERO = ImageFont.load_default()

def ease_in_out(t):
    return t * t * (3.0 - 2.0 * t)

def ease_out(t):
    return 1 - (1 - t) ** 3

def lerp(a, b, t):
    return a + (b - a) * t

def render_frame(f):
    img = Image.new("RGB", (WIDTH, HEIGHT), C_BG)
    draw = ImageDraw.Draw(img, "RGBA")
    
    # --------------------------------------------------------
    # SECTION 1: CHAOS / ENTROPY (Frames 0 - 80) ~ 2.6s
    # --------------------------------------------------------
    if f < 80:
        alpha = min(1.0, f / 20.0)
        tag_color = (*C_ACCENT, int(255 * alpha))
        draw.text((80, 50), "01 // CHAOS", font=FONT_TAG, fill=tag_color)
        
        # Draw lines between dots
        line_alpha = int(70 * alpha)
        for i1, i2 in CHAOS_PAIRS:
            x1, y1 = SCATTER_X[i1], SCATTER_Y[i1]
            x2, y2 = SCATTER_X[i2], SCATTER_Y[i2]
            draw.line([(x1, y1), (x2, y2)], fill=(*C_LIGHT, line_alpha), width=1)
            
        # Draw drifting dots
        dot_color = (*C_LIGHT, int(220 * alpha))
        drift_t = f * 0.04
        for i in range(NUM_DOTS):
            dx = math.sin(drift_t + i) * 6
            dy = math.cos(drift_t + i * 1.5) * 6
            x, y = SCATTER_X[i] + dx, SCATTER_Y[i] + dy
            draw.ellipse([x - 4, y - 4, x + 4, y + 4], fill=dot_color)

    # --------------------------------------------------------
    # SECTION 2: CONVERGENCE / FOCUS (Frames 80 - 160) ~ 2.6s
    # --------------------------------------------------------
    elif f < 160:
        prog = (f - 80) / 80.0
        t_snap = ease_in_out(min(1.0, prog * 1.3))
        
        draw.text((80, 50), "02 // FOCUS", font=FONT_TAG, fill=(*C_ACCENT, 255))
        
        cx, cy = WIDTH // 2, HEIGHT // 2
        radius = 160
        
        # Center core glow & circle
        core_scale = ease_out(min(1.0, prog * 1.5))
        r_inner = int(radius * core_scale)
        if r_inner > 2:
            # Outer ring
            draw.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner],
                         outline=(*C_ACCENT, int(200 * core_scale)), width=3)
            # Center bright hub
            hub_r = int(22 * core_scale)
            draw.ellipse([cx - hub_r, cy - hub_r, cx + hub_r, cy + hub_r], fill=(*C_ACCENT, 255))
            
        # Converge dots into orbit
        orbit_angle_base = prog * 1.5
        for i in range(NUM_DOTS):
            target_angle = orbit_angle_base + i * (2 * math.pi / NUM_DOTS)
            tx = cx + radius * math.cos(target_angle)
            ty = cy + radius * math.sin(target_angle)
            
            x = lerp(SCATTER_X[i], tx, t_snap)
            y = lerp(SCATTER_Y[i], ty, t_snap)
            draw.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(*C_LIGHT, 240))
            
        # Pulsing shockwave ring at climax
        if prog > 0.5:
            p_prog = (prog - 0.5) / 0.5
            p_rad = radius + p_prog * 250
            p_alpha = int(200 * (1.0 - p_prog))
            draw.ellipse([cx - p_rad, cy - p_rad, cx + p_rad, cy + p_rad],
                         outline=(*C_ACCENT, p_alpha), width=2)

    # --------------------------------------------------------
    # SECTION 3: PIPELINE / ARCHITECTURE (Frames 160 - 240) ~ 2.6s
    # --------------------------------------------------------
    elif f < 240:
        prog = (f - 160) / 80.0
        draw.text((80, 50), "03 // PIPELINE", font=FONT_TAG, fill=(*C_ACCENT, 255))
        
        # 4 Nodes horizontally
        node_xs = [280, 520, 760, 1000]
        node_y = HEIGHT // 2
        
        # Connectors
        line_grow = min(1.0, prog * 1.5)
        for i in range(len(node_xs) - 1):
            x_start = node_xs[i] + 45
            x_end = lerp(x_start, node_xs[i+1] - 45, line_grow)
            draw.line([(x_start, node_y), (x_end, node_y)], fill=(*C_LIGHT, 180), width=3)
            
        # Nodes
        for i, nx in enumerate(node_xs):
            node_appear = min(1.0, max(0.0, (prog * 3.0) - (i * 0.4)))
            if node_appear > 0:
                side = int(45 * ease_out(node_appear))
                # Fill dark bg, outline light
                draw.rectangle([nx - side, node_y - side, nx + side, node_y + side],
                               fill=C_BG, outline=(*C_LIGHT, 255), width=2)
                # Inner square detail
                in_side = side // 2
                draw.rectangle([nx - in_side, node_y - in_side, nx + in_side, node_y + in_side],
                               fill=(*C_ACCENT, int(150 * node_appear)))

        # Energy pulse running across nodes
        if prog > 0.35:
            packet_prog = ((prog - 0.35) * 2.2) % 1.0
            px = lerp(node_xs[0], node_xs[-1], packet_prog)
            draw.ellipse([px - 9, node_y - 9, px + 9, node_y + 9], fill=(*C_ACCENT, 255))
            draw.ellipse([px - 16, node_y - 16, px + 16, node_y + 16], outline=(*C_ACCENT, 100), width=2)

    # --------------------------------------------------------
    # SECTION 4: OUTCOME / EXPONENTIAL IMPACT (Frames 240 - 330) ~ 3.0s
    # --------------------------------------------------------
    else:
        prog = (f - 240) / 90.0
        draw.text((80, 50), "04 // OUTCOME", font=FONT_TAG, fill=(*C_ACCENT, 255))
        
        # Clean geometric chart bounds
        ox, oy = 250, 550
        gw, gh = 750, 380
        
        axis_t = min(1.0, prog * 2.0)
        # Axes
        draw.line([(ox, oy), (ox + gw * axis_t, oy)], fill=(*C_LIGHT, 120), width=2)
        draw.line([(ox, oy), (ox, oy - gh * axis_t)], fill=(*C_LIGHT, 120), width=2)
        
        # Subtle horizontal gridlines
        for k in range(1, 4):
            gy = oy - (gh / 4) * k
            draw.line([(ox, gy), (ox + gw * axis_t, gy)], fill=(*C_LIGHT, 25), width=1)
            
        # Exponential curve
        curve_prog = ease_out(min(1.0, max(0.0, (prog - 0.15) * 1.5)))
        if curve_prog > 0:
            curve_pts = []
            steps = int(120 * curve_prog)
            for s in range(steps + 1):
                cur_x = s / 120.0
                # Exponential curve formula
                cur_y = 0.08 * math.exp(2.5 * cur_x)
                px = ox + cur_x * gw
                py = oy - (cur_y / 1.0) * (gh * 0.95)
                curve_pts.append((px, py))
                
            if len(curve_pts) > 1:
                # Shaded area under the curve
                poly_pts = [(ox, oy)] + curve_pts + [(curve_pts[-1][0], oy)]
                draw.polygon(poly_pts, fill=(*C_ACCENT, 35))
                
                # Main glow line
                draw.line(curve_pts, fill=(*C_ACCENT, 255), width=4)
                
                # Peak beacon
                head_x, head_y = curve_pts[-1]
                draw.ellipse([head_x - 8, head_y - 8, head_x + 8, head_y + 8], fill=(*C_ACCENT, 255))
                draw.ellipse([head_x - 18, head_y - 18, head_x + 18, head_y + 18], outline=(*C_LIGHT, 160), width=2)
                
                # "10x" Stat Reveal
                if curve_prog > 0.85:
                    label_alpha = int(255 * min(1.0, (curve_prog - 0.85) / 0.15))
                    draw.text((head_x - 150, head_y - 65), "10x", font=FONT_HERO, fill=(*C_LIGHT, label_alpha))

    return np.array(img)

def main():
    print(f"Rendering {TOTAL_FRAMES} frames ({TOTAL_FRAMES/FPS:.1f}s) via FFmpeg...")
    
    ffmpeg_cmd = [
        "ffmpeg",
        "-y",
        "-f", "rawvideo",
        "-vcodec", "rawvideo",
        "-s", f"{WIDTH}x{HEIGHT}",
        "-pix_fmt", "rgb24",
        "-r", str(FPS),
        "-i", "-",
        "-c:v", "libx264",
        "-pix_fmt", "yuv420p",
        "-preset", "fast",
        "-crf", "18",
        OUTPUT_FILE
    ]
    
    proc = subprocess.Popen(ffmpeg_cmd, stdin=subprocess.PIPE, stderr=subprocess.PIPE)
    
    for f in range(TOTAL_FRAMES):
        frame_bytes = render_frame(f).tobytes()
        proc.stdin.write(frame_bytes)
        if f % 60 == 0:
            print(f"  Frame {f}/{TOTAL_FRAMES} ({f/TOTAL_FRAMES*100:.0f}%)")
            
    proc.stdin.close()
    proc.wait()
    print(f"[SUCCESS] Video successfully saved as: {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
