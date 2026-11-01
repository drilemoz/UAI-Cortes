# Validação local — v0.1.88 (versionCode 89)

## Mudanças

- `ProjectBackupPolicy.kt`: intervalos de 15/30/60/120/300 s e retenção de 3/5/10, identificador de pasta por SHA-256.
- `ProjectBackups.kt`: histórico local com `AtomicFile`, checksum SHA-256, SAF externo opcional, importação/exportação JSON por projeto, limites de tamanho e recuperação de erro.
- `ProjectBackupLifecycle.kt`: snapshots quando a edição está ativa, ao trocar de tela e em `ON_STOP`. O agendador só funciona enquanto o processo do app estiver ativo; projeto principal já é salvo a cada mudança.
- `ProjectBackupSettingsScreen.kt`: interface para configurar, exportar, importar e recuperar versões com confirmação.
- `ProjectMediaAvailability.kt`: alerta de URI inacessível (ausência ou revogação de permissão), sem remover dados.
- Navegação em `MainActivity`, `HomeScreens`, `EditorNavigation`; versionamento Gradle, workflow, README, CHANGELOG e notas.

## Limitações

- **Não** copia vídeos, fotos ou áudios para backups JSON, nem restaura arquivos excluídos. A funcionalidade de copiar fontes importadas continua pendente.
- Histórico privado desaparece se os dados do app forem apagados; backup SAF externo pode sobreviver, dependendo da pasta escolhida.
- Não é backup em nuvem, não faz temporização exata com o app encerrado. Exportação permanece em `Movies/UaiCortes` no Android moderno.
- Gradle/Android Lint e teste de restauração no Android real pendentes. Validação estrutural/sintática e testes de política não substituem compilação.
