package io.mosip.greetings.cryptography;

import java.security.SecureRandom;

class WalletCryptoBoxImpl implements WalletCryptoBox {
    private final CryptoBox selfCryptoBox;
    private SecureRandom secureRandom;

    WalletCryptoBoxImpl(SecureRandom random) {
        this.selfCryptoBox = new CryptoBoxBuilder().setSecureRandomSeed(random).build();
        this.secureRandom = random;
    }

    @Override
    public byte[] publicKey() {
        return selfCryptoBox.getPublicKey();
    }

    @Override
    public SecretsTranslator buildSecretsTranslator(byte[] verifierPublicKey) {
        byte[] ivBytes = new byte[CryptoBox.INITIALISATION_VECTOR_LENGTH];
        secureRandom.nextBytes(ivBytes);

        CipherBoxPackage cipherBoxPackage = selfCryptoBox.createCipherBoxes(verifierPublicKey, KeyGenerator.WALLET_INFO, KeyGenerator.VERIFIER_INFO, ivBytes);
        return new SenderTransfersOwnershipOfData(ivBytes, cipherBoxPackage);
    }
}
