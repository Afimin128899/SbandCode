"""Palette regression checks; these do not replace Android UI or build tests."""

from pathlib import Path
import re
import unittest


THEME_FILE = (
    Path(__file__).resolve().parents[1]
    / "app/src/main/kotlin/app/sbandcode/android/ui/Theme.kt"
)


def palette():
    return {
        name: int(value, 16)
        for name, value in re.findall(
            r"(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)",
            THEME_FILE.read_text(),
        )
    }


def luminance(argb):
    channels = [(argb >> shift & 255) / 255 for shift in (16, 8, 0)]
    linear = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return sum(c * weight for c, weight in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(first, second):
    low, high = sorted((luminance(first), luminance(second)))
    return (high + 0.05) / (low + 0.05)


class ThemeTests(unittest.TestCase):
    def test_reference_contrast(self):
        self.assertAlmostEqual(contrast(0xFFFFFFFF, 0xFF000000), 21)
        self.assertAlmostEqual(contrast(0xFF14161C, 0xFF14161C), 1)

    def test_text_roles_are_readable(self):
        colors = palette()
        for background in (
            "primary", "primaryContainer", "secondary", "secondaryContainer",
            "tertiary", "tertiaryContainer", "background", "surface",
            "surfaceVariant", "error", "errorContainer",
        ):
            foreground = "on" + background[0].upper() + background[1:]
            with self.subTest(background=background):
                self.assertGreaterEqual(contrast(colors[foreground], colors[background]), 4.5)
        self.assertGreaterEqual(contrast(colors["inverseOnSurface"], colors["inverseSurface"]), 4.5)

    def test_supporting_text_on_raised_surfaces(self):
        colors = palette()
        for background in (
            "surface", "surfaceContainerLowest", "surfaceContainerLow",
            "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest",
        ):
            with self.subTest(background=background):
                self.assertGreaterEqual(contrast(colors["onSurfaceVariant"], colors[background]), 4.5)
        self.assertGreaterEqual(contrast(colors["outline"], colors["surface"]), 4.5)

    def test_error_has_its_own_color(self):
        colors = palette()
        red, green, blue = [(colors["error"] >> shift & 255) for shift in (16, 8, 0)]
        self.assertGreater(red, green)
        self.assertGreater(red, blue)
        self.assertNotEqual(colors["error"], colors["primary"])


if __name__ == "__main__":
    unittest.main()
