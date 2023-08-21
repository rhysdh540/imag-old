# imag

a simple command line tool to optimise the file sizes of `.png`, `.nbt`, and `.ogg` files

## how does it work?

it just uses a couple of *other* command line tools to do the heavy lifting:

- for `png` files:
  - [Oxipng](https://github.com/shssoichiro/oxipng)
  - [ZopfliPNG](https://github.com/google/zopfli)
  - [PNGOUT](http://advsys.net/ken/utils.htm)
  - PNGFix (which i *believe* is a part of [libpng](http://www.libpng.org/pub/png/libpng.html)?)
- for `ogg` files:
  - [OptiVorbis](https://github.com/OptiVorbis/OptiVorbis)
- for `nbt` files:
  - [Zopfli](https://github.com/google/zopfli)

## licenses
since i package all the above with the program, here are their licenses:

<details>
  <summary>click to view Oxipng license</summary>

  Oxipng is licensed under the MIT license.<br>
  See it [here](https://github.com/shssoichiro/oxipng/blob/master/LICENSE)
</details>
<details>
  <summary>click to view Zopfli/ZopfliPNG license</summary>

  Zopfli is licensed under the Apache License 2.0.<br>
  See it [here](https://github.com/google/zopfli/blob/master/COPYING)
</details>
<details>
  <summary>click to view PNGOUT license</summary>

  The author of PNGOUT, Ken Silverman, has requested that I put his name and [site](http://advsys.net/ken/utils.htm) "clearly displayed in some reasonable fashion that can be seen by an average user." I hope this is good enough. 
</details>
<details>
  <summary>click to view libpng license</summary>

  libpng (and therefore also PNGFix) is licensed under the libpng license.<br>
  See it [here](http://www.libpng.org/pub/png/src/libpng-LICENSE.txt)
</details>
<details>
  <summary>click to view OptiVorbis license</summary>

  OptiVorbis is licensed under the GNU Affero General Public License.<br>
  See it [here](https://github.com/OptiVorbis/OptiVorbis/blob/master/LICENSE)
</details>

all of them are compatible with the GNU General Public License v3.0, so i'm using that here.