package dev.morewater.qalab.config;

/**
 * Configuración de las pruebas. Orden de prioridad: propiedad de Maven (-Dqalab.user=...) → variable de entorno → valor por defecto.
 * Así el mismo código corre en tu máquina, en GitHub Actions, en Jenkins y en Azure Pipelines.
 */
public final class Config {
    private Config() {}

    private static String get(String prop, String env, String fallback) {
        String p = System.getProperty(prop);
        if (p != null && !p.isBlank() && !p.startsWith("${")) return p;
        String e = System.getenv(env);
        return e != null && !e.isBlank() ? e : fallback;
    }

    /** Sitio bajo prueba. */
    public static String baseUrl() { return get("qalab.url", "QALAB_URL", "https://qalab.morewater.dev").replaceAll("/+$", ""); }
    /** Usuario de prueba de qalab: estandar, lento, intermitente, visual, amnesia, expira, descuadre, bloqueado. */
    public static String user() { return get("qalab.user", "QALAB_USER", "estandar"); }
    public static String password() { return get("qalab.password", "QALAB_PASSWORD", "qalab123"); }
    public static boolean headless() { return Boolean.parseBoolean(get("qalab.headless", "QALAB_HEADLESS", "true")); }
    /** Si se define, se usa un navegador remoto (Selenium Grid / contenedor selenium/standalone-chromium). */
    public static String remoteUrl() { return get("qalab.remote", "SELENIUM_REMOTE_URL", ""); }
    /** Ruta de chromedriver cuando el navegador viene del sistema (p. ej. dentro de la imagen de Jenkins). */
    public static String chromedriverPath() { return get("qalab.chromedriver", "CHROMEDRIVER_PATH", ""); }
    /** Ruta del navegador cuando es Chromium del sistema (p. ej. /usr/bin/chromium). */
    public static String chromeBinary() { return get("qalab.chrome", "CHROME_BIN", ""); }
    public static int timeoutSeconds() { return Integer.parseInt(get("qalab.timeout", "QALAB_TIMEOUT", "15")); }
}
