use std::num::NonZeroU64;
use jni::{
    JNIEnv,
    objects::{
        JClass,
        JPrimitiveArray,
    },
    sys::{
        jbyteArray,
        jboolean,
    },
};
use zopfli::{
    self,
    Options as ZopfliOptions,
    Format as ZopfliFormat,
};
use oxipng::{self, optimize_from_memory, Options as OxipngOptions, Options};

#[no_mangle]
pub extern "system" fn Java_dev_rdh_imag_processors_impl_OxiPngProcessor_compress(
    mut env: JNIEnv, _class: JClass, data: jbyteArray, alpha: jboolean
) -> jbyteArray {
    let array = unsafe { JPrimitiveArray::from_raw(data) };
    let data = env.convert_byte_array(array).unwrap();

    let options = OxipngOptions {
        optimize_alpha: alpha != 0,
        ..Options::max_compression()
    };

    let result = match optimize_from_memory(&data, &options) {
        Ok(data) => data,
        Err(_) => {
            env.throw_new("java/lang/Exception", "Failed to compress in oxipng").expect("Failed to throw java exception");
            return env.byte_array_from_slice(&[]).unwrap().into_raw();
        },
    };

    return env.byte_array_from_slice(&result).unwrap().into_raw();
}

#[no_mangle]
pub extern "system" fn Java_dev_rdh_imag_processors_impl_archives_GZipProcessor_compress(
    mut env: JNIEnv, _class: JClass, data: jbyteArray,
) -> jbyteArray {
    let array = unsafe { JPrimitiveArray::from_raw(data) };
    let data = env.convert_byte_array(array).unwrap();

    let options = ZopfliOptions {
        iteration_count: NonZeroU64::new(1000).unwrap(),
        ..Default::default()
    };

    let mut result = Vec::with_capacity(data.len());

    match zopfli::compress(options, ZopfliFormat::Gzip, &*data, &mut result) {
        Ok(_) => (),
        Err(_) => env.throw_new("java/lang/Exception", "Failed to compress in zopfli").expect("Failed to throw java exception"),
    };

    return env.byte_array_from_slice(&result).unwrap().into_raw();
}