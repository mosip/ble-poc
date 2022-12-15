package io.mosip.greetings.cryptography;

public interface VerifierCryptoBox {
    byte[] publicKey();
    SecretsTranslator buildCommunicator(byte[] initializationVector, byte[] walletPublicKey);
}
