package dev.morewater.qalab.core;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/** Cierra los PDF de evidencia cuando termina la sesión de JUnit (registrado por ServiceLoader). */
public class EvidenceSession implements LauncherSessionListener {
    @Override
    public void launcherSessionClosed(LauncherSession session) {
        Evidence.closeAll();
    }
}
