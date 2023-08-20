# imag

a simple command line tool to optimise the file sizes of `.png`, `.nbt`, and `.ogg` files

## how does it work?

it just uses a couple of *other* command line tools to do the heavy lifting:

- for `png` files:
  - [OxiPNG](https://github.com/shssoichiro/oxipng)
  - [ZopfliPNG](https://github.com/google/zopfli)
  - [PNGOUT](http://advsys.net/ken/utils.htm)
  - PNGFix (which i *believe* is a part of [libpng](http://www.libpng.org/pub/png/libpng.html)?)
- for `ogg` files:
  - [OptiVorbis](https://github.com/OptiVorbis/OptiVorbis)
- for `nbt` files:
  - [Zopfli](https://github.com/google/zopfli)