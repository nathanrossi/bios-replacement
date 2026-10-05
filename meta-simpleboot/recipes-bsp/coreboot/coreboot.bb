SUMMARY = "Fast, secure and flexible OpenSource firmware"
HOMEPAGE = "https://www.coreboot.org"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=751419260aa954499f7abaabaa882bbe"

SRC_URI = "gitsm://github.com/coreboot/coreboot.git;protocol=https;name=coreboot;branch=main"
SRCREV_coreboot = "8b0e34f90e08547cf19e550a43ed72f69c39d6ed"
PV = "26.09+git+${SRCPV}"

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

PLATFORM_BINARY_COMPONENTS = "${THISDIR}/../../../cwwk-adl-6l/ifd-extract"

do_configure() {
    # Copy image components into workdir
    cp ${PLATFORM_BINARY_COMPONENTS}/flashregion_0_flashdescriptor.bin ${WORKDIR}/flashregion_0_flashdescriptor.bin
    cp ${PLATFORM_BINARY_COMPONENTS}/flashregion_2_intel_me.bin ${WORKDIR}/flashregion_2_intel_me.bin

    echo "" > ${B}/.config
    append_cfg() {
        echo "$*" >> ${B}/.config
    }

    append_cfg "CONFIG_COMPILER_GCC=y"
    append_cfg "CONFIG_ANY_TOOLCHAIN=y"
    append_cfg "CONFIG_LOCALVERSION=\"${COREBOOT_LOCALVERSION}\""

    # Payload is added in a separate recipe
    append_cfg "CONFIG_PAYLOAD_NONE=y"

    append_cfg "CONFIG_VENDOR_CWWK=y"
    append_cfg "CONFIG_BOARD_CWWK_CW_ADL_6L_V1_1=y"

    # Flash Descriptor
    append_cfg "CONFIG_HAVE_IFD_BIN=y"
    append_cfg "CONFIG_IFD_BIN_PATH=\"${WORKDIR}/flashregion_0_flashdescriptor.bin\""

    # Management Engine
    append_cfg "CONFIG_HAVE_ME_BIN=y"
    append_cfg "CONFIG_ME_BIN_PATH=\"${WORKDIR}/flashregion_2_intel_me.bin\""

    # Increase CBFS to fit within BIOS segment of IFD
    append_cfg "CONFIG_CBFS_SIZE=0xa00000"

    # Enable TPM2 support via Intel fTPM/CSME
    append_cfg "CONFIG_TPM2=y"

    # Enable CBFS component hash integrity checking
    append_cfg "CONFIG_CBFS_VERIFICATION=y"

    append_cfg "CONFIG_DEFAULT_CONSOLE_LOGLEVEL_5=y"

    # Debugging
    # append_cfg "CONFIG_DEFAULT_CONSOLE_LOGLEVEL_8=y"
    # append_cfg "CONFIG_DISPLAY_FSP_CALLS_AND_STATUS=y"
    # append_cfg "CONFIG_DISPLAY_FSP_HEADER=y"

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
