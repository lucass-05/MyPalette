import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.example.palette.MainKt"

        // el instalador se genera con :desktopApp:packageReleaseDistributionForCurrentOS.
        // la version release pasa el codigo por proguard, que lo encoge pero suele romper librerias
        // como ktor o coil si no se le dan reglas. para probar la app no hace falta, asi que se apaga
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // nombre con el que se instala y aparece en el menu de inicio
            packageName = "Palette"
            packageVersion = "1.0.0"
            description = "Lo que ves, juegas, lees y escuchas, convertido en tu paleta"
            vendor = "Palette"

            // el instalador lleva dentro su propio java. por defecto va recortado a lo minimo y la red
            // puede fallar por un modulo que falte; con todos los modulos pesa mas pero no da sorpresas
            includeAllModules = true

            // un icono por sistema, cada uno en su formato
            windows {
                iconFile.set(project.file("icons/palette.ico"))
                menuGroup = "Palette"
                shortcut = true
                // identifica la app para windows: con el mismo uuid, instalar una version nueva sustituye a la anterior
                upgradeUuid = "9307503D-0023-4538-B33D-E5642C7BCF19"
            }
            macOS {
                iconFile.set(project.file("icons/palette.icns"))
            }
            linux {
                iconFile.set(project.file("icons/palette.png"))
            }
        }
    }
}
