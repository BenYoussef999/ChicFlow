package com.chicflow.tunisia.network;

/**
 * The ONLY place where the server address is written.
 *
 * BASE_URL must end with a slash and point to the "api" folder of the backend.
 *
 *  - Android EMULATOR + XAMPP on your PC : "http://10.0.2.2/chicflow/api/"
 *  - REAL PHONE + deployed server         : "https://your-domain.tn/api/"   (https is required)
 *
 * ("localhost" does NOT work from the phone/emulator: it would mean the phone itself.)
 */
public class ApiConfig {

    public static final String BASE_URL = "http://10.0.2.2/chicflow/api/";

    // Endpoint names (we add more in each stage)
    public static final String PING = "ping.php";

    // Auth (stage 2)
    public static final String REGISTER = "auth/register.php";
    public static final String LOGIN    = "auth/login.php";
    public static final String LOGOUT   = "auth/logout.php";
    public static final String PROFILE  = "auth/profile.php";

    private ApiConfig() { }
}
