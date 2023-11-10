use std::num::NonZeroU64;

use jni::{
    JNIEnv,
    objects::{
        JClass,
        JPrimitiveArray,
    },
    sys::{
        jboolean,
        jbyteArray,
    },
};
use optivorbis::Remuxer;
use optivorbis::remuxer::ogg_to_ogg as ogg2ogg;

use oxipng::{
    Options as OxipngOptions,
    optimize_from_memory
};

use zopfli::{
    self,
    Options as ZopfliOptions,
    Format as ZopfliFormat,
};


//private static native byte[] compress(byte[] data, boolean alpha);
#[no_mangle]
pub extern "system" fn Java_dev_rdh_imag_processors_impl_OxiPngProcessor_compress<'local>(
    mut env: JNIEnv, _class: JClass, data: jbyteArray, alpha: jboolean
) -> jbyteArray {
    let array = unsafe { JPrimitiveArray::from_raw(data) };
    let data = env.convert_byte_array(array).unwrap();

    let options = OxipngOptions {
        optimize_alpha: alpha != 0,
        ..Default::default()
    };

    let result = match optimize_from_memory(&data, &options) {
        Ok(data) => data,
        Err(_) => {
            env.throw_new("java/lang/Exception", "Failed to compress in oxipng").expect("Failed to throw java exception");
            Vec::new()
        }
    };

    return env.byte_array_from_slice(&result).unwrap().into_raw();
}

//private static native byte[] compress(byte[] data);
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

    let mut result: Vec<u8> = Vec::with_capacity(data.len());

    match zopfli::compress(options, ZopfliFormat::Gzip, &*data, &mut result) {
        Ok(_) => (),
        Err(_) => env.throw_new("java/lang/Exception", "Failed to compress in zopfli").expect("Failed to throw java exception"),
    };

    return env.byte_array_from_slice(&result).unwrap().into_raw();
}