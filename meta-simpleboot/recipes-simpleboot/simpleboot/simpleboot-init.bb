SUMMARY = "simpleboot"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit allarch

SRC_URI = " \
    file://init \
    "

S = "${UNPACKDIR}"

do_install() {
    # Create device nodes expected by some kernels in initramfs before even
    # executing /init.
    install -d ${D}/dev
    mknod -m 622 ${D}/dev/console c 5 1

    # point init at simpleboot script
    install -d ${D}${sbindir}
    install -m 0755 ${S}/init ${D}${sbindir}/init
    # ln -sf ${sbindir}/init ${D}/init
}

FILES:${PN} += "/init /dev"
RDEPENDS:${PN} += "kexec"
RDEPENDS:${PN} += "util-linux-agetty"
