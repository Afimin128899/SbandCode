#!/system/bin/sh
# The agent's shell: Alpine Linux from the app's files, run through proot (fake root,
# hard links turned into symlinks, which Android forbids). The phone's own folders are
# bound at the same paths, so the working directory means the same inside and out.
export PROOT_LOADER="$SBANDCODE_PROOT_LOADER" PROOT_TMP_DIR="$SBANDCODE_PROOT_TMP"
case "${0##*/}" in
    git) set -- /usr/bin/git "$@" ;;
    *) set -- /bin/sh "$@" ;;
esac
exec "$SBANDCODE_PROOT" --kill-on-exit -0 --link2symlink -r "$SBANDCODE_ROOTFS" \
    -b /dev -b /proc -b /sys -b /system -b /apex -b /storage -b /sdcard \
    -b "$SBANDCODE_FILES" -b "$SBANDCODE_CACHE" -w "$(pwd)" \
    /usr/bin/env -u LD_LIBRARY_PATH -u LD_PRELOAD \
    PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin \
    HOME=/root TMPDIR=/tmp LANG=C.UTF-8 TERM=xterm-256color SHELL=/bin/sh \
    "$@"
