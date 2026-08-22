pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS: nenhum módulo pode declarar repositórios próprios;
    // tudo resolve por aqui. Snapshots do design system saem do repo de snapshots
    // do Sonatype (útil enquanto a lib não tem release estável).
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content { includeGroup("io.github.matheusbrum") }
        }
    }
}

rootProject.name = "by-concerts-android"

// ── Design System via COMPOSITE BUILD ────────────────────────────────────────
// O mns-design-system é um build Gradle independente, FORA da árvore deste
// projeto — por isso usamos includeBuild (não include(":modulo")). O composite
// build substitui automaticamente a coordenada Maven declarada no catálogo
// (io.github.matheusbrum:mns-design-system) pela build local.
//
// A substituição é explícita porque o Gradle deriva a coordenada de um projeto
// incluído do seu NOME (:design_system), enquanto a lib publica com artifactId
// "mns-design-system". Mapeamos a coordenada Maven do catálogo para o projeto
// local — é o mesmo group/artifact que o artefato terá no Maven.
//
// FUTURO: ao publicar a lib no Maven, basta remover este bloco includeBuild; a
// mesma dependência do catálogo passará a resolver o artefato remoto, sem tocar
// no código de consumo das features.
includeBuild("/Users/matheusbrum/StudioProjects/mns-design-system") {
    dependencySubstitution {
        substitute(module("io.github.matheusbrum:mns-design-system"))
            .using(project(":design_system"))
    }
}

// ── Módulos do app ───────────────────────────────────────────────────────────
include(":app")
include(":core:common")
include(":core:ui")
include(":domain")
include(":data")
include(":payment")
include(":feature:events")
include(":feature:checkout")
