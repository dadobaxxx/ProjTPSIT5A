from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
import binascii

# 1) Generare le chiavi RSA
def generate_keys():
    key = RSA.generate(2048)
    private_key = key.export_key()
    public_key = key.publickey().export_key()
    return private_key, public_key

# 2) Criptare un testo
def encrypt_message(public_key, message):
    rsa_public_key = RSA.import_key(public_key)
    cipher = PKCS1_OAEP.new(rsa_public_key)
    encrypted_message = cipher.encrypt(message.encode())
    return binascii.hexlify(encrypted_message)

# 3) Decriptare un testo
def decrypt_message(private_key, encrypted_message):
    rsa_private_key = RSA.import_key(private_key)
    cipher = PKCS1_OAEP.new(rsa_private_key)
    decrypted_message = cipher.decrypt(binascii.unhexlify(encrypted_message))
    return decrypted_message.decode()

# Esempio di utilizzo
private_key, public_key = generate_keys()
print("Chiave pubblica:", public_key.decode())
print("Chiave privata:", private_key.decode())

message = "Ciao, mondo!"
encrypted_message = encrypt_message(public_key, message)
print("Messaggio criptato:", encrypted_message.decode())

decrypted_message = decrypt_message(private_key, encrypted_message)
print("Messaggio decriptato:", decrypted_message)
