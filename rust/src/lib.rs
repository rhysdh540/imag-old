use jni::JNIEnv;

use jni::objects::{JClass};

use jni::sys::{jstring};

#[no_mangle]
pub extern "system" fn Java_dev_rdh_imag_Main_test(env: JNIEnv, _class: JClass) -> jstring {
    println!("Hello from Rust!");
    return env.new_string("Rust String")
        .expect("Couldn't create java string!")
        .into_raw();
}