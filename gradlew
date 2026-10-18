#!/usr/bin/env sh
# Quando existe a distribuição do Gradle instalada, utiliza-a.
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
if [ -f "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -classpath "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi
echo 'Gradle não está instalado. Execute o workflow do GitHub Actions ou instale Gradle 8.11.1.' >&2
exit 1
