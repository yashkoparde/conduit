"""
DemoSlides: Minimal Text, Maximum Visual Manim Presentation
Pallete (Strict 3 Colors):
- BG / Deep Tone:      #0D1117 (Dark Obsidian)
- Accent / Glow Tone:  #FF6B4A (Vibrant Coral / Energy)
- Light / Crisp Tone:  #F0F6FC (Off-white / Clarity)

Run with:
    manim -pql demo_slides.py VisualDemoSlides
Or high quality:
    manim -pqh demo_slides.py VisualDemoSlides
"""

from manim import *

# ==========================================
# 3-COLOR PALETTE DEFINITION
# ==========================================
C_BG = "#0D1117"       # Canvas / Background
C_ACCENT = "#FF6B4A"   # Primary Visual Highlight & Action
C_LIGHT = "#F0F6FC"    # Crisp structural elements & minimal labels

config.background_color = C_BG


class VisualDemoSlides(Scene):
    def construct(self):
        # ----------------------------------------------------
        # SLIDE 1: PROBLEM / FRAGMENTATION (Entropy & Noise)
        # ----------------------------------------------------
        # Visual: Scattered chaotic dots & broken connections
        scattered_dots = VGroup(*[
            Dot(
                point=[np.random.uniform(-5.5, 5.5), np.random.uniform(-3, 3), 0],
                radius=0.08,
                color=C_LIGHT
            )
            for _ in range(45)
        ])
        
        # Chaotic lines
        chaos_lines = VGroup(*[
            Line(
                scattered_dots[i].get_center(),
                scattered_dots[(i + 7) % len(scattered_dots)].get_center(),
                stroke_width=1.0,
                stroke_opacity=0.35,
                color=C_LIGHT
            )
            for i in range(25)
        ])

        slide1_tag = Text("01 // CHAOS", font="sans-serif", weight=BOLD, font_size=20, color=C_ACCENT)
        slide1_tag.to_corner(UL, buff=0.8)

        self.play(FadeIn(slide1_tag, shift=DOWN * 0.2), run_time=0.6)
        self.play(
            LaggedStart(
                *[Create(dot) for dot in scattered_dots],
                lag_ratio=0.02
            ),
            Create(chaos_lines),
            run_time=2.0
        )
        self.wait(0.8)

        # ----------------------------------------------------
        # SLIDE 2: CONVERGENCE / THE CORE (Focus & Synthesis)
        # ----------------------------------------------------
        # Minimal label transition
        slide2_tag = Text("02 // FOCUS", font="sans-serif", weight=BOLD, font_size=20, color=C_ACCENT)
        slide2_tag.to_corner(UL, buff=0.8)

        # Target ring & core anchor
        core_circle = Circle(radius=1.8, stroke_color=C_ACCENT, stroke_width=3)
        core_glow = Annulus(inner_radius=1.8, outer_radius=2.1, color=C_ACCENT, fill_opacity=0.25, stroke_width=0)
        center_hub = Dot(ORIGIN, radius=0.22, color=C_ACCENT)

        # Target positions in orbit around the core
        orbit_dots = VGroup()
        for i, dot in enumerate(scattered_dots):
            angle = i * (2 * PI / len(scattered_dots))
            r = 1.8
            target_pos = np.array([r * np.cos(angle), r * np.sin(angle), 0])
            orbit_dots.add(dot.animate.move_to(target_pos).set_color(C_LIGHT))

        self.play(
            Transform(slide1_tag, slide2_tag),
            FadeOut(chaos_lines, scale=0.5),
            *orbit_dots,
            Create(core_circle),
            FadeIn(core_glow),
            GrowFromCenter(center_hub),
            run_time=2.2
        )
        self.wait(0.5)

        # Pulse wave radiating outwards
        pulse_wave = Circle(radius=0.2, stroke_color=C_ACCENT, stroke_width=4)
        self.play(
            pulse_wave.animate.scale(15).set_stroke(opacity=0),
            rate_func=linear,
            run_time=1.4
        )
        self.remove(pulse_wave)

        # ----------------------------------------------------
        # SLIDE 3: SCALED ARCHITECTURE / NETWORK PIPELINE
        # ----------------------------------------------------
        slide3_tag = Text("03 // PIPELINE", font="sans-serif", weight=BOLD, font_size=20, color=C_ACCENT)
        slide3_tag.to_corner(UL, buff=0.8)

        # Transition core into clean multi-stage flow nodes
        node_coords = [
            np.array([-4.2, 0, 0]),
            np.array([-1.4, 0, 0]),
            np.array([1.4, 0, 0]),
            np.array([4.2, 0, 0]),
        ]

        nodes = VGroup(*[
            Square(side_length=1.1, stroke_color=C_LIGHT, stroke_width=2.5, fill_color=C_BG, fill_opacity=0.9)
            .move_to(pt)
            for pt in node_coords
        ])

        connectors = VGroup(*[
            Line(node_coords[i] + RIGHT * 0.55, node_coords[i+1] + LEFT * 0.55, stroke_color=C_LIGHT, stroke_width=2.5)
            for i in range(len(node_coords) - 1)
        ])

        # Morph everything previous into clean pipeline structure
        self.play(
            Transform(slide1_tag, slide3_tag),
            FadeOut(scattered_dots),
            FadeOut(core_circle),
            FadeOut(core_glow),
            FadeOut(center_hub),
            run_time=0.8
        )

        self.play(
            LaggedStart(
                *[GrowFromCenter(node) for node in nodes],
                lag_ratio=0.15
            ),
            Create(connectors),
            run_time=1.6
        )

        # Streaming energy packets through the pipeline
        packets = VGroup(*[
            Dot(node_coords[0], radius=0.15, color=C_ACCENT)
            for _ in range(3)
        ])

        for packet in packets:
            self.play(
                packet.animate.move_to(node_coords[-1]),
                rate_func=rush_into,
                run_time=0.8
            )
            self.play(
                Flash(node_coords[-1], color=C_ACCENT, line_length=0.3, num_lines=8, flash_radius=0.6),
                run_time=0.3
            )
            self.remove(packet)

        # ----------------------------------------------------
        # SLIDE 4: EXPONENTIAL OUTCOME / IMPACT (Max Visual)
        # ----------------------------------------------------
        slide4_tag = Text("04 // OUTCOME", font="sans-serif", weight=BOLD, font_size=20, color=C_ACCENT)
        slide4_tag.to_corner(UL, buff=0.8)

        # Minimalist high-impact geometric graph
        axes = Axes(
            x_range=[0, 6, 1],
            y_range=[0, 10, 2],
            x_length=8.5,
            y_length=4.5,
            axis_config={"color": C_LIGHT, "stroke_width": 2, "include_ticks": False},
        ).shift(DOWN * 0.3)

        curve = axes.plot(
            lambda x: 0.15 * np.exp(0.72 * x),
            x_range=[0, 5.8],
            color=C_ACCENT,
            stroke_width=5
        )

        # Shaded area under curve for visual richness
        area = axes.get_area(curve, x_range=[0, 5.8], color=C_ACCENT, opacity=0.18)

        peak_indicator = Dot(axes.c2p(5.8, 0.15 * np.exp(0.72 * 5.8)), radius=0.2, color=C_ACCENT)
        peak_ring = Annulus(
            inner_radius=0.22,
            outer_radius=0.45,
            color=C_LIGHT,
            stroke_width=0,
            fill_opacity=0.35
        ).move_to(peak_indicator.get_center())

        stat_label = Text("10x", font="sans-serif", weight=HEAVY, font_size=60, color=C_LIGHT)
        stat_label.next_to(peak_indicator, UP + LEFT, buff=0.35)

        self.play(
            Transform(slide1_tag, slide4_tag),
            FadeOut(nodes),
            FadeOut(connectors),
            Create(axes),
            run_time=1.2
        )

        self.play(
            Create(curve),
            FadeIn(area),
            run_time=1.8,
            rate_func=ease_out_cubic
        )

        self.play(
            GrowFromCenter(peak_indicator),
            FadeIn(peak_ring, scale=1.5),
            Write(stat_label),
            run_time=1.0
        )
        self.wait(1.5)

        # Fade out to elegant finish
        self.play(
            FadeOut(VGroup(slide1_tag, axes, curve, area, peak_indicator, peak_ring, stat_label)),
            run_time=1.2
        )
        self.wait(0.5)
