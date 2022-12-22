//
//  KeyGenerator.swift
//  bluetoothdemo
//
//  Created by ShreeThaanu on 16/12/22.
//

import Foundation
import CryptoKit

class KeyGenerator {

    func generateStrongKeyBasedOnHKDF(sharedSecretKey: SharedSecret, keyLength: Int, infoData: String) -> SymmetricKey {
        let salt = "SHA-256".data(using: .utf8)
        let sharedInfo = infoData.data(using: .utf8)
        let strongKey = sharedSecretKey.hkdfDerivedSymmetricKey(using: SHA256.self, salt: salt!, sharedInfo: sharedInfo!, outputByteCount: keyLength)
        return strongKey
    }
}
