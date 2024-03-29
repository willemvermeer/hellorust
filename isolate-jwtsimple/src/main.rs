use jwt_simple::prelude::*;
use models::IdClaims;
use std::fs;
use std::time::Instant;

mod models {
    use serde::{Deserialize, Serialize};

    #[derive(Debug, Serialize, Deserialize)]
    pub struct IdClaims {
        pub username: String,
        pub email: String,
    }
}

fn main() {
    let start = Instant::now();

    let n1 = start.elapsed();
    let private_pem_file_content = fs::read_to_string("privatekey-authx.pkcs8")
        .expect("Should have been able to read the file");
    // println!("{}", private_pem_file_content);
    let key_pair =
        RS256KeyPair::from_pem(&private_pem_file_content).expect("Could not read private key");
    let n11 = start.elapsed();
    println!("Elapsed load keypair {}", (n11 - n1).as_millis());

    let mut totalMicros: u128 = 0;
    let testCount = 1000;

    let mut i = 0;
    while i < testCount {
        let n2 = start.elapsed();
        let id_claims = IdClaims {
            username: "NPA-PlatformManagement".to_string(),
            email: "usr001..".to_string(),
        };
        let claims =
            Claims::with_custom_claims(id_claims, coarsetime::Duration::from_secs(60 * 60 * 2))
                .with_issuer("https://authx.xlinq.io")
                .with_audience("scf.xlinq.io");
        let token = key_pair.sign(claims).expect("Could not sign claims");
        let n3 = start.elapsed();
        if(i == 0) {
            println!("Elapsed sign {} token {}", (n3 - n2).as_micros(), token);
        }
        i = i + 1;
        totalMicros += (n3 - n2).as_micros()
    }
    let averageMicors = totalMicros / testCount;
    println!("Average {} micros for {} tests", averageMicors, testCount);
}
