#!/bin/bash

# Script d'automatisation des tests et validations
echo "🚀 Démarrage de la validation complète de CashRuler"

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULTS_DIR="$PROJECT_DIR/validation-results"
DATE=$(date +"%Y-%m-%d_%H-%M")
LOG_FILE="$RESULTS_DIR/validation_$DATE.log"

# Création du dossier de résultats
mkdir -p "$RESULTS_DIR"

# Fonction de logging
log() {
    echo "[$(date +"%H:%M:%S")] $1" | tee -a "$LOG_FILE"
}

# Nettoyage
log "🧹 Nettoyage de l'environnement..."
./gradlew clean >> "$LOG_FILE" 2>&1

# Vérification du code
log "📝 Analyse statique du code..."
./gradlew ktlintCheck >> "$LOG_FILE" 2>&1
KTLINT_RESULT=$?

# Tests unitaires
log "🧪 Exécution des tests unitaires..."
./gradlew testDebugUnitTest >> "$LOG_FILE" 2>&1
UNIT_TEST_RESULT=$?

# Tests d'intégration
log "🔄 Exécution des tests d'intégration..."
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cashruler.validation.ValidationSuite >> "$LOG_FILE" 2>&1
INTEGRATION_TEST_RESULT=$?

# Tests de performance
log "⚡ Exécution des tests de performance..."
./gradlew :app:benchmarkAppStart >> "$LOG_FILE" 2>&1
PERFORMANCE_TEST_RESULT=$?

# Génération des rapports
log "📊 Génération des rapports..."
./gradlew jacocoTestReport >> "$LOG_FILE" 2>&1

# Vérification des résultats
check_results() {
    local test_name="$1"
    local result="$2"
    if [ $result -eq 0 ]; then
        log "✅ $test_name: OK"
    else
        log "❌ $test_name: ÉCHEC"
        GLOBAL_SUCCESS=false
    fi
}

GLOBAL_SUCCESS=true

check_results "Analyse statique" $KTLINT_RESULT
check_results "Tests unitaires" $UNIT_TEST_RESULT
check_results "Tests d'intégration" $INTEGRATION_TEST_RESULT
check_results "Tests de performance" $PERFORMANCE_TEST_RESULT

# Compilation du rapport final
REPORT_FILE="$RESULTS_DIR/validation_summary_$DATE.md"

cat << EOF > "$REPORT_FILE"
# Rapport de Validation CashRuler

Date: $(date)

## Résultats des Tests

| Type de Test | Statut |
|--------------|--------|
| Analyse statique | $([ $KTLINT_RESULT -eq 0 ] && echo "✅" || echo "❌") |
| Tests unitaires | $([ $UNIT_TEST_RESULT -eq 0 ] && echo "✅" || echo "❌") |
| Tests d'intégration | $([ $INTEGRATION_TEST_RESULT -eq 0 ] && echo "✅" || echo "❌") |
| Tests de performance | $([ $PERFORMANCE_TEST_RESULT -eq 0 ] && echo "✅" || echo "❌") |

## Métriques

- Couverture de code: $(grep -r "Total.*%" "$PROJECT_DIR/app/build/reports/coverage" | tail -1 | awk '{print $NF}')
- Tests exécutés: $(grep -r "Tests run:" "$PROJECT_DIR/app/build/reports/tests" | awk '{sum += $3} END {print sum}')
- Durée totale: $(date -d@$SECONDS -u +%H:%M:%S)

## Détails des Rapports

Les rapports détaillés sont disponibles dans les emplacements suivants:
- Tests unitaires: app/build/reports/tests/
- Couverture: app/build/reports/coverage/
- Lint: app/build/reports/lint/
- Performance: app/build/reports/benchmarks/

EOF

# Notification du résultat
if [ "$GLOBAL_SUCCESS" = true ]; then
    log "✨ Validation terminée avec succès!"
    exit 0
else
    log "⚠️ La validation a échoué. Consultez les rapports pour plus de détails."
    exit 1
fi
