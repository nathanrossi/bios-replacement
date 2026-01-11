SUMMARY = "Assemble the flash image for the device"
LICENSE = "MIT"

DEPENDS = "coreboot-tools-native"

FLASH_IMAGE_BASENAME ?= "flash-${MACHINE}"
FLASH_IMAGE_FULLNAME ?= "${FLASH_IMAGE_BASENAME}-${DATETIME}.rom"
FLASH_IMAGE_FULLNAME[vardepsexclude] = "DATETIME"

inherit deploy

B = "${WORKDIR}/build"

do_configure[noexec] = "1"

do_compile[depends] += " \
    lib32-coreboot:do_deploy \
    virtual/kernel:do_deploy \
    "

do_compile() {
    cp ${DEPLOY_DIR_IMAGE}/coreboot-${MACHINE}.rom ${B}/flash.rom

    cbfstool \
        ${B}/flash.rom \
        add-payload \
        -f ${DEPLOY_DIR_IMAGE}/bzImage-initramfs-${MACHINE}.bin \
        -n fallback/payload  \
        -c none  \
        -r COREBOOT \
        -C "console=ttyS0,115200"
}

do_deploy() {
    install -m 0644 ${B}/flash.rom ${DEPLOYDIR}/${FLASH_IMAGE_FULLNAME}
    ln -sf ${FLASH_IMAGE_FULLNAME} ${DEPLOYDIR}/${FLASH_IMAGE_BASENAME}.rom
}
addtask deploy before do_build after do_install

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "common-x86-64"
