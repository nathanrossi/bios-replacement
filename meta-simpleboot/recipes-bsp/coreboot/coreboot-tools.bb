SUMMARY = "Fast, secure and flexible OpenSource firmware"
HOMEPAGE = "https://www.coreboot.org"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=751419260aa954499f7abaabaa882bbe"

SRC_URI = "git://github.com/coreboot/coreboot.git;protocol=https;name=coreboot;branch=main"
SRCREV_coreboot = "7edcb87323097f0e6ddd86af9e33a9726261b943"
SRCREV_FORMAT = "coreboot"
PV = "25.12+git+${SRCPV}"

SRC_URI += "git://github.com/coreboot/vboot.git;protocol=https;destsuffix=vboot;name=vboot;nobranch=1"
SRCREV_vboot = "3f94e2c7ed58c4e67d6e7dc6052ec615dbbb9bb4"

B = "${WORKDIR}/${BPN}"

inherit deploy

EXTRA_OEMAKE += "obj=${B}"
EXTRA_OEMAKE += "objutil=${B}"
EXTRA_OEMAKE += "V=1"
EXTRA_OEMAKE += "VBOOT_SOURCE=${S}/../vboot"

do_configure[noexec] = "1"

do_compile() {
    export HOSTCC="${CC}"
    export HOSTCFLAGS="${TARGET_CFLAGS}"
    # Coreboot ignores all usual conventions for specifying the toolchain to use.
    # Its logic for toolchain selection is in util/xcompile/xcompile.
    # This is as close as we can reasonably get to making it use our Yocto toolchain:
    export CROSS_COMPILE="${TARGET_PREFIX}"

    oe_runmake -C ${S}/util/cbfstool all
}

do_install() {
    oe_runmake -C ${S}/util/cbfstool install PREFIX=${PREFIX} BINDIR=${bindir} DESTDIR=${D}
}

BBCLASSEXTEND = "native"

# cbfs depends on compiling a target native object (x86)
COMPATIBLE_HOST = "(x86_64.*|i.86.*)"
