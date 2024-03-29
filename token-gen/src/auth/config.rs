use jsonwebtoken::{DecodingKey, EncodingKey};

pub struct AuthConfig {
    pub encoding_key: EncodingKey,
    pub decoding_key: Option<DecodingKey>,
    pub issuer: String,
    pub audience: String,
}
