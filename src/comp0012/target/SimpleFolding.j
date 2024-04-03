; Jasmin Java assembler code that assembles the SimpleFolding example class

.source Type1.j
.class public comp0012/target/SimpleFolding
.super java/lang/Object

.method public <init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public simple()V
	.limit stack 3

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 67
	ldc 12345
    iadd
    invokevirtual java/io/PrintStream/println(I)V
	return
.end method

.method public floatMul()V
	.limit stack 3

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 10.0
	ldc 5.0
    fmul
    invokevirtual java/io/PrintStream/println(F)V
	return
.end method


.method public longAdd()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc2_w 1000
	ldc2_w 500
    ladd
    invokevirtual java/io/PrintStream/println(J)V
	return
.end method

.method public floatDiv()V
	.limit stack 3

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 10.0
	ldc 5.0
    fdiv
    invokevirtual java/io/PrintStream/println(F)V
	return
.end method

.method public doubleDiv()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc2_w 4.0                                  
	ldc2_w 20.0                                  
	ddiv 
    invokevirtual java/io/PrintStream/println(D)V
	return
.end method
