# miniJava Compiler

## Introduction
During the Spring 2022 semester, I had the opportunity to take COMP 520 (Compilers) at UNC. Our entire semester was dedicated to building a compiler for miniJava (a subset of Java) from scratch. 

This miniJava compiler is responsible for validating the syntax and semantics of a miniJava program and then generating machine code that is interpreted by the mJAM machine. The compiler takes one miniJava file as input and then compiles and executes the miniJava program. If any error is encountered throughout the program, an exception will be thrown and the program will be terminated. Below, I give a more detailed breakdown of each phase of the compiler. 

*Note: All instructions for the assignment are in the instructions folder.*

## Breakdown

### PA1
The first part of the compiler is responsible for making sure the syntax of the miniJava program is valid. Every programming language has a syntax it follows (you can see miniJava's syntax at the bottom of the PA1.pdf file in the instructions folder), and before compiling the miniJava code further, we need to ensure that the program is valid. 

The syntactic analyzer can be broken into two parts: a scanner and a parser. The scanner is responsible turning the source code into tokens. We then use a parser to parse through the generated tokens and ensure that the program is following the correct syntax. The entire point of generating tokens is so that it is easy to parse.  If there are any syntax errors, an exception will be thrown and the program will be terminated.

### PA2 
After making sure the program was syntactically valid, the next job of the compiler is to build an Abstract Syntax Tree (AST). Essentially, an AST builds a top-down tree which shows the relationship between all the tokens in a tree format. This helps check the logic of the actual program and allows us to contextually analyze the program which includes validating scopes, typechecking, ensuring functions are being used correctly, if/else condition flows, and other semantics of the program. AST generation is a critical step which allows the compiler to easily walk through the program and validate the logic.

### PA3
After the AST is constructed, the compiler performs contextual analysis. As stated above, contextual analysis is a key step in the compilation process. The contextual analyzer is different from the syntactical analyzer because the contextual analyzer is responsible for validating the semantics of the program, whereas the syntactical analyzer is responsible for validating the syntax of the program. 

Contextual analysis of the miniJava program consists of identification and type checking of the AST. Identification is the process of identifying where a name (a variable or function) is declared and then mapping any uses of that name to the original declaration. This also involves checking other information such as scope. Type checking involves validating that variables throughout the program use the correct types. 

During this step, the AST is further modified to include more details. If successful, the compiler moves onto code generation. Otherwise, exceptions will be thrown for any errors found in this step of the compilation process. 

### PA4
In the final step of the miniJava compilation process, mJAM instructions are generated which are then interpreted by the mJAM machine resulting in the execution of the code. In order to do this, a final traversal of the AST is required. This will generate the instructions and then generate all mJAM instructions in a .mJAM file. This file can then be passed to the interpreter and executed. 
