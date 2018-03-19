## Bert for Android

[![Release](https://jitpack.io/v/softwarejoint/AndroidBert.svg)](https://jitpack.io/#softwarejoint/AndroidBert)

Implements binary Erlang external format in java for android.

For Other Languages

* [Swift](https://github.com/softwarejoint/BERTswift)
* [Android](https://github.com/softwarejoint/AndroidBert)
* [JavaScript](https://github.com/softwarejoint/BERT-JS)

### Android => Erlang

```
             Android                          Erlang
    ------------------------------------------------------------
             Boolean                          Atom
             Atom                             Atom
             Byte                             Number
             byte[]                           Binary
             Short                            Number
             Int                              Number
             Long                             Number
             BigInteger                       Number
             Float                            Number
             Double => Float                  Number
             String                           List | Binary
             Native Arrays                    List
             List                             List
             BertTuple (List)                 Tuple
             Map                              Map

```

### Erlang => Android

```
             Erlang                       Android
    ------------------------------------------------------------
             true | false                 Boolean
             Atom                         BertAtom | String
             Number                       Byte | Short | Integer | Long | Float | Double | BigInteger
             Binary                       byte[] | String
             List                         ArrayList | String
             PropList                     ArrayList (BertTuple) | Map <K, V>
             Tuple                        BertTuple
             Map                          LinkedHashMap
```


### Notes

#### Encoding
* String can be encoded as either list or binary.
* Maps keys can be forced to either Atom or String or Term 

#### Decoding
* Atom can be forced to either Boolean | BertAtom or String

### Using in your project

1. Include jitpack.io maven repo

```
    repositories {
        maven { url "https://jitpack.io" }
    }

```

2. Add dependency to project

```
    dependencies {
	    compile 'com.github.softwarejoint:AndroidBert:1.0.1'
	}

```
