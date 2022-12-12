package io.mosip.greetings.cryptography;

import java.security.SecureRandom;

public class CryptoBoxBuilder {

    private SecureRandom secureRandomSeed;
    public CryptoBoxBuilder setSecureRandomSeed(SecureRandom secureRandomSeed) {
        this.secureRandomSeed = secureRandomSeed;
        return this;
    }

    public CryptoBox build(){
        if(secureRandomSeed == null)
            throw new RuntimeException("Cannot create cryptobox without secure random seed");

        return new CryptoBoxImpl(secureRandomSeed);
    }
}
