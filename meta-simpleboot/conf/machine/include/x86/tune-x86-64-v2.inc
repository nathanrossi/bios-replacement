# Settings for the GCC(1)/psABI cpu-type "x86-64-v2":
DEFAULTTUNE ?= "x86-64-v2"

# Include the previous tune to pull in PACKAGE_EXTRA_ARCHS
require conf/machine/include/x86/tune-corei7.inc

# Extra tune features
TUNEVALID[x86-64-v2] = "Enable x86-64-v2 specific processor optimizations"
TUNE_CCARGS .= "${@bb.utils.contains('TUNE_FEATURES', 'x86-64-v2', ' -march=x86-64-v2', '', d)}"

# Extra tune selections
AVAILTUNES += "x86-64-v2"
TUNE_FEATURES:tune-x86-64-v2 = "${TUNE_FEATURES:tune-x86-64} x86-64-v2"
BASE_LIB:tune-x86-64-v2 = "lib64"
TUNE_PKGARCH:tune-x86-64-v2 = "x86-64-v2"
PACKAGE_EXTRA_ARCHS:tune-x86-64-v2 = "${PACKAGE_EXTRA_ARCHS:tune-corei7-64} x86-64-v2"
