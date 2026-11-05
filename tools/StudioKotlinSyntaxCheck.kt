import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

fun main(args: Array<String>) {
    setIdeaIoUseFallback()
    val disposable = Disposer.newDisposable()
    try {
        val environment = KotlinCoreEnvironment.createForProduction(disposable,
            CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val psi = KtPsiFactory(environment.project)
        var errors = 0
        args.forEach { fileName ->
            val file = File(fileName)
            val tree = psi.createFile(file.name, file.readText())
            val invalid = PsiTreeUtil.collectElementsOfType(tree, PsiErrorElement::class.java)
            invalid.forEach { err ->
                System.err.println("${file.name}: ${err.textOffset}: ${err.errorDescription}: ${err.text.take(50)}")
                errors++
            }
        }
        println("Kotlin PSI: ${args.size} arquivos, $errors erros sintáticos")
        if (errors > 0) error("Falha de sintaxe")
    } finally { Disposer.dispose(disposable) }
}
