#!/bin/sh
set -e
here=$(cd "$(dirname "$0")" && pwd)
rsvg-convert "$here/icon.svg" -o "$here/sbandcode-1024.png"
