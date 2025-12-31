SUMMARY = "Fast, secure and flexible OpenSource firmware"
HOMEPAGE = "https://www.coreboot.org"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=751419260aa954499f7abaabaa882bbe"

SRC_URI = "gitsm://github.com/coreboot/coreboot.git;protocol=https;name=coreboot;branch=main"
SRCREV_coreboot = "5a0207e56a4dbf599a2268fdc43ca3ab78b3af26"
PV = "24.05+git+${SRCPV}"

S = "${WORKDIR}/git"
B = "${WORKDIR}/coreboot-${MACHINE}"

COREBOOT_IMAGE_BASENAME ?= "coreboot-${MACHINE}"
COREBOOT_IMAGE_FULLNAME ?= "${COREBOOT_IMAGE_BASENAME}-${DATETIME}.rom"
COREBOOT_IMAGE_FULLNAME[vardepsexclude] = "DATETIME"

COREBOOT_LOCALVERSION ?= "${DISTRO_EXTENDED_VERSION}"

inherit deploy
inherit pkgconfig

EXCLUDE_FROM_WORLD = "1"
INHIBIT_DEFAULT_DEPS = "1"

DEPENDS = " \
    iasl-native \
    kern-tools-native \
    openssl-native \
    bc-native \
    virtual/cross-cc \
    ${MLPREFIX}libgcc \
    nss-native \
    openssl-native \
    "

EXTRA_OEMAKE += "HOSTPKGCONFIG=pkg-config-native"
EXTRA_OEMAKE += "-C ${S}"
EXTRA_OEMAKE += "obj=${B}"
EXTRA_OEMAKE += "DOTCONFIG=${B}/.config"
EXTRA_OEMAKE += "V=1"

PACKAGE_ARCH = "${MACHINE_ARCH}"

# menuconfig support
inherit cml1
KCONFIG_CONFIG_COMMAND = "${EXTRA_OEMAKE} menuconfig"

do_configure() {
    echo "" > ${B}/.config
    append_cfg() {
        echo "$*" >> ${B}/.config
    }

    append_cfg "CONFIG_COMPILER_GCC=y"
    append_cfg "CONFIG_ANY_TOOLCHAIN=y"
    append_cfg "CONFIG_LOCALVERSION=\"${COREBOOT_LOCALVERSION}\""

    # append_cfg "CONFIG_PAYLOAD_NONE=y"
    append_cfg "CONFIG_PAYLOAD_LINUX=y"
    append_cfg "CONFIG_PAYLOAD_FILE=\"${DEPLOY_DIR_IMAGE}/bzImage-initramfs-${MACHINE}.bin\""
    append_cfg "CONFIG_COMPRESSED_PAYLOAD_NONE=y"
    append_cfg "CONFIG_LINUX_COMMAND_LINE=\"console=ttyS0,115200\""

    append_cfg "CONFIG_VENDOR_CWWK=y"
    append_cfg "CONFIG_BOARD_CWWK_CW_ADL_6L_V1_1=y"

    # TODO: reference files relative to git repo
    # Flash Descriptor
    append_cfg "CONFIG_HAVE_IFD_BIN=y"
    append_cfg "CONFIG_IFD_BIN_PATH=\"/home/nathan/dev/bios-replacement/cwwk-adl-6l/ifd-extract/flashregion_0_flashdescriptor.bin\""

    # Management Engine
    append_cfg "CONFIG_HAVE_ME_BIN=y"
    append_cfg "CONFIG_ME_BIN_PATH=\"/home/nathan/dev/bios-replacement/cwwk-adl-6l/ifd-extract/flashregion_2_intel_me.bin\""

    # Increase CBFS to fit within BIOS segment of IFD
    append_cfg "CONFIG_CBFS_SIZE=0xa00000"

    # CONFIG_CBFS_VERIFICATION?

    # Debugging
    append_cfg "CONFIG_DEFAULT_CONSOLE_LOGLEVEL_8=y"
    append_cfg "CONFIG_DISPLAY_FSP_CALLS_AND_STATUS=y"
    append_cfg "CONFIG_DISPLAY_FSP_HEADER=y"
    append_cfg "CONFIG_DEBUG_CBFS=y"

    oe_runmake olddefconfig
}

do_compile() {
    # Inject --sysroot via xcompile
    sed -i 's@\(CC_RT_GCC=.*GCC.\) .*\(..CFLAGS\)@\1 --sysroot=${STAGING_DIR_TARGET} \2@g' ${S}/util/xcompile/xcompile

    # for access to BIN_PATH files
    export WORKDIR=${WORKDIR}

    export HOSTCC="${BUILD_CC}"
    export HOSTCFLAGS="${BUILD_CFLAGS}"
    # Coreboot ignores all usual conventions for specifying the toolchain to use.
    # Its logic for toolchain selection is in util/xcompile/xcompile.
    # This is as close as we can reasonably get to making it use our Yocto toolchain:
    export CROSS_COMPILE="${HOST_PREFIX}"

    oe_runmake all
}

do_deploy() {
    install -m 0644 ${B}/coreboot.rom ${DEPLOYDIR}/${COREBOOT_IMAGE_FULLNAME}
    ln -sf ${COREBOOT_IMAGE_FULLNAME} ${DEPLOYDIR}/${COREBOOT_IMAGE_BASENAME}.rom
}
addtask deploy before do_build after do_install

COMPATIBLE_MACHINE = "common-x86-64"
