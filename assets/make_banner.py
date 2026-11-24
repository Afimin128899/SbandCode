#!/usr/bin/env python3
"""Render the SbandCode banner."""

import pathlib
import subprocess

HERE = pathlib.Path(__file__).parent

SVG = """<svg xmlns="http://www.w3.org/2000/svg" width="900" height="260" viewBox="0 0 900 260">
  <defs>
    <linearGradient id="fade">
      <stop stop-color="#0d0d0d"/>
      <stop offset="1" stop-color="#262626"/>
    </linearGradient>
    <pattern id="grid" width="28" height="28" patternUnits="userSpaceOnUse">
      <path d="M28 0H0V28" fill="none" stroke="#373737" stroke-width="1"/>
    </pattern>
  </defs>
  <rect width="900" height="260" fill="url(#fade)"/>
  <rect x="616" width="284" height="260" fill="url(#grid)" opacity=".6"/>
  <rect x="58" y="53" width="154" height="154" rx="34" fill="#191919" stroke="#494949" stroke-width="2"/>
  <g transform="translate(135 130) scale(2.05) translate(-54 -54)">
    <path d="M68 31 H45 L32 44 V58 H58 V61 L52 67 H32 V77 H56 L68 65 V48 H43 V47 L49 41 H68 Z M71 67 H80 V77 H71 Z" fill="#f2f2f2"/>
  </g>
  <text x="253" y="145" font-family="Noto Sans" font-weight="700" font-size="73" fill="#F4F4F4">SbandCode</text>
  <text x="257" y="185" font-family="Noto Sans" font-size="24" fill="#B9B9B9">OpenCode for Android</text>
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
