# Lava6
## Overview
Lava6 is a little java virtual machine.  Some features:
- This is a 16-bit machine and uses char extensively.
- Internally it uses a type called Num which holds either an int or float and which is stored in 4 chars (1 for the type, and 3 for the value).  
- It has a memory (char array) of size 4096, which is plenty for my simple test programs.  The memory holds Strings (char arrays), Nums (char arrays), 
Arrays, and Tables (which hold constant items, and references to Fields and Methods).
- This has an internal language called Ident, which converts any String to a char by encoding the first 4 letters.
- Compiler reads in the Java class file and stores it in memory as chars.  This has a table which holds some of the ConstantPool items.
- It has special constants to refer to system functions such as println and parseint.

# Usage
To use it, just call Lava with the Java class file and arguments.  This depends on bcel-6.5.0.jar from Apache BCEL.

# #Status:
This implements only a small subset of the Java opcodes.  For example, it can't even do multiplication, although that would be fairly easy to implement. 
This can't do static class initialization.  I don't intend on improving the code in this version.  It is a checkpoint release.  The code needs refactored.
But it is a good start and it compiles and works for simple code.
