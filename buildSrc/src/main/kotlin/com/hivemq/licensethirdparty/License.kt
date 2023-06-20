package com.hivemq.licensethirdparty

interface License {
    val fullName: String
    val url: String?
}

enum class KnownLicense(val id: String, override val fullName: String, override val url: String) : License {
    APACHE_2_0("Apache-2.0", "Apache License 2.0", "https://spdx.org/licenses/Apache-2.0.html"),
    BOUNCY_CASTLE("MIT", "Bouncy Castle Licence", "https://www.bouncycastle.org/licence.html"),
    BSD_2_CLAUSE("BSD-2-Clause", "BSD 2-Clause \"Simplified\" License", "https://spdx.org/licenses/BSD-2-Clause.html"),
    BSD_3_CLAUSE("BSD-3-Clause", "BSD 3-Clause \"New\" or \"Revised\" License", "https://spdx.org/licenses/BSD-3-Clause.html"),
    CC0_1_0("CC0-1.0", "Creative Commons Zero v1.0 Universal", "https://spdx.org/licenses/CC0-1.0.html"),
    CDDL_1_0("CDDL-1.0", "Common Development and Distribution License 1.0", "https://spdx.org/licenses/CDDL-1.0.html"),
    CDDL_1_1("CDDL-1.1", "Common Development and Distribution License 1.1", "https://spdx.org/licenses/CDDL-1.1.html"),
    // EDL has BSD-3-Clause as SPDX id, documented in the following links:
    // https://spdx.org/licenses/BSD-3-Clause.html
    // https://www.eclipse.org/org/documents/edl-v10.php
    // https://lists.spdx.org/g/Spdx-legal/topic/request_for_adding_eclipse/67981884
    EDL_1_0("BSD-3-Clause", "Eclipse Distribution License - v 1.0", "https://www.eclipse.org/org/documents/edl-v10.php"),
    EPL_1_0("EPL-1.0", "Eclipse Public License 1.0", "https://spdx.org/licenses/EPL-1.0.html"),
    EPL_2_0("EPL-2.0", "Eclipse Public License 2.0", "https://spdx.org/licenses/EPL-2.0.html"),
    GO("BSD-3-Clause", "Go License", "https://golang.org/LICENSE"),
    MIT("MIT", "MIT License", "https://spdx.org/licenses/MIT.html"),
    MIT_0("MIT-0", "MIT No Attribution", "https://spdx.org/licenses/MIT-0.html"),
    PUBLIC_DOMAIN("Public Domain", "Public Domain", ""),
    W3C_19980720("W3C-19980720", "W3C Software Notice and License (1998-07-20)", "https://spdx.org/licenses/W3C-19980720.html"),
}

data class UnknownLicense(override val fullName: String, override val url: String?): License
