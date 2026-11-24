#!/usr/bin/env python3
"""Render the SbandCode banner."""

import pathlib
import subprocess

HERE = pathlib.Path(__file__).parent

SVG = """<svg xmlns="http://www.w3.org/2000/svg" width="900" height="260" viewBox="0 0 900 260">
  <rect width="900" height="260" fill="#131313"/>
  <rect x="65" y="32" width="185" height="185" rx="58" fill="none" stroke="#444444" stroke-width="2"/>
  <g transform="translate(157.5 124.5) scale(2.25) translate(-54 -54)">
    <path d="M37,32 H71 Q77,32 77,38 V70 Q77,76 71,76 H37 Q31,76 31,70 V38 Q31,32 37,32 Z" fill="none" stroke="#fff" stroke-width="3.5"/><path d="M38,43 L49,54 L38,65 M55,65 L70,65" fill="none" stroke="#fff" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
  </g>
  <text x="285" y="148" font-family="Noto Sans" font-weight="700" font-size="82" fill="#F4F4F4">SbandCode</text>
  <text x="288" y="198" font-family="Noto Sans" font-size="30" fill="#BBBBBB">opencode on your phone</text>
</svg>"""


def main():
    source = HERE / "banner.svg"
    source.write_text(SVG)
    out = HERE / "banner.png"
    subprocess.run(["rsvg-convert", "-w", "900", "-h", "260", str(source), "-o", str(out)], check=True)
    source.unlink()
    print("wrote", out)


if __name__ == "__main__":
    main()
