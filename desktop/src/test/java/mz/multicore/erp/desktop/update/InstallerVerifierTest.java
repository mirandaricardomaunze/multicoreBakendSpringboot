package mz.multicore.erp.desktop.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AU-03 (size and SHA-256) and AU-04 (signature only once a signer is configured). */
class InstallerVerifierTest {

    @TempDir Path dir;

    private Path installer(String content) throws Exception {
        Path file = dir.resolve("Multicore-1.0.4.exe.part");
        Files.writeString(file, content);
        return file;
    }

    private static UpdateManifest manifestOf(Path file) throws Exception {
        return new UpdateManifest("1.0.4", InstallerVerifier.sha256(file), Files.size(file));
    }

    @Test
    void matchingSizeAndHash_withoutSigner_accepted_andSignatureNeverRead() throws Exception {
        Path file = installer("instalador verdadeiro");
        AtomicInteger reads = new AtomicInteger();
        InstallerVerifier verifier = new InstallerVerifier("", f -> {
            reads.incrementAndGet();
            return null;
        });

        assertThatCode(() -> verifier.verify(file, manifestOf(file))).doesNotThrowAnyException();
        assertThat(verifier.requiresSignature()).isFalse();
        assertThat(reads).hasValue(0);
        assertThat(file).exists();
    }

    @Test
    void differentHash_refusedAndDeleted() throws Exception {
        Path file = installer("instalador trocado no caminho");
        UpdateManifest published = new UpdateManifest("1.0.4", "b".repeat(64), Files.size(file));

        assertThatThrownBy(() -> new InstallerVerifier("", f -> null).verify(file, published))
                .isInstanceOf(UpdateException.class).hasMessageContaining("SHA-256");
        assertThat(file).doesNotExist();
    }

    @Test
    void differentSize_refusedAndDeleted() throws Exception {
        Path file = installer("curto");
        UpdateManifest published = new UpdateManifest("1.0.4", InstallerVerifier.sha256(file), 999);

        assertThatThrownBy(() -> new InstallerVerifier("", f -> null).verify(file, published))
                .hasMessageContaining("999");
        assertThat(file).doesNotExist();
    }

    @Test
    void withSigner_validSignatureFromThatSigner_accepted() throws Exception {
        Path file = installer("assinado");
        InstallerVerifier verifier = new InstallerVerifier("CN=Multicore, Lda",
                f -> new InstallerVerifier.Signature("Valid", "CN=Multicore, Lda, O=Multicore, C=MZ"));

        assertThatCode(() -> verifier.verify(file, manifestOf(file))).doesNotThrowAnyException();
    }

    @Test
    void withSigner_unsignedOrOtherSigner_refusedEvenWithMatchingHash() throws Exception {
        Path unsigned = installer("sem assinatura");
        UpdateManifest manifest = manifestOf(unsigned);
        assertThatThrownBy(() -> new InstallerVerifier("CN=Multicore, Lda",
                f -> new InstallerVerifier.Signature("NotSigned", "")).verify(unsigned, manifest))
                .hasMessageContaining("assinatura digital válida");
        assertThat(unsigned).doesNotExist();

        Path other = installer("assinado por outro");
        UpdateManifest otherManifest = manifestOf(other);
        assertThatThrownBy(() -> new InstallerVerifier("CN=Multicore, Lda",
                f -> new InstallerVerifier.Signature("Valid", "CN=Outra Empresa")).verify(other, otherManifest))
                .hasMessageContaining("Outra Empresa");

        Path silent = installer("sem resposta do Windows");
        UpdateManifest silentManifest = manifestOf(silent);
        assertThatThrownBy(() -> new InstallerVerifier("CN=Multicore, Lda", f -> null).verify(silent, silentManifest))
                .hasMessageContaining("sem resposta");
    }

    @Test
    void shippedTrustPolicy_isReadable() {
        // Blank today (no certificate yet). Filling it in switches every update to signature-required.
        assertThatCode(UpdateTrust::expectedSigner).doesNotThrowAnyException();
    }
}
