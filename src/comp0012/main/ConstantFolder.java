package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;


public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here
		//System.out.println("optimising general\n");
		//System.out.println("class name: " + cgen.getClassName() + "\n");



		if ((cgen.getClassName()).equals("comp0012.target.SimpleFolding"))
		{
			//System.out.println("optimising simple folding\n");

			Method[] methods = gen.getMethods();
			int count = 0;

			for (Method method : methods) {
				//System.out.println("method\n");

				/*
				if (count == 0)
				{
					count++;
					System.out.println("skipping the 1st method");
					continue;
				}
				*/

				MethodGen methodGen = new MethodGen(method, gen.getClassName(), cpgen);
				InstructionList instructionList = methodGen.getInstructionList();


				System.out.println(" ########### printing all directly copied original instructions ########### \n");

				for (InstructionHandle ih : instructionList.getInstructionHandles()) {
					Instruction inst = ih.getInstruction();
					System.out.println(inst.toString()+ "\n");
				}

				System.out.println(" ########### printing all directly copied constants ########### \n");
				ConstantPool cp = cpgen.getConstantPool();
				for (int i = 0; i < cp.getLength(); i++) {
					Constant constant = cp.getConstant(i);
					if (constant != null) {
						System.out.println(i + ": " + constant + "\n");
					}
				}

				if (instructionList != null) {
					for (InstructionHandle ih : instructionList.getInstructionHandles()) {
						//System.out.println("instruction\n");

						Instruction instruction = ih.getInstruction();

						// Check for arithmetic instructions
						if (instruction instanceof IADD)
						{
							InstructionHandle prev1 = ih.getPrev();
							InstructionHandle prev2 = prev1 != null ? prev1.getPrev() : null;
							if (prev1 != null && prev2 != null) {
								Instruction prevInst1 = prev1.getInstruction();
								Instruction prevInst2 = prev2.getInstruction();

								if ((prevInst1 instanceof LDC) && (prevInst2 instanceof LDC))
								{
									//System.out.println("found 2 loading instructions \n");

									Object obj1 = ((LDC) prevInst1).getValue(cpgen);
									Object obj2 = ((LDC) prevInst2).getValue(cpgen);

									if ((obj1 instanceof Integer) && (obj2 instanceof Integer))
									{

										//System.out.println("adding 2 integers\n");

										int cnstValue = (Integer) obj1 + (Integer) obj2;
										System.out.println("number: " + cnstValue + "\n");

										int index = cpgen.addInteger(cnstValue);
										// Load the calculated value onto the stack
										instructionList.insert(ih, new LDC(index));

										//instructionList.insert(ih, new PUSH(cpgen, cnstValue)); //adding the instruction at the correct handle
										//System.out.println("added new instruction\n");

										try{
											instructionList.delete(ih);
											//System.out.println("deleted add\n");
										} catch (TargetLostException e) {
											throw new RuntimeException(e);
										}
										try {
											instructionList.delete(prev1);
											//System.out.println("deleted 1st load\n");
										} catch (TargetLostException e) {
											throw new RuntimeException(e);
										}
										try {
											instructionList.delete(prev2);
											//System.out.println("deleted 2nd load\n");
										}  catch (TargetLostException e) {
											throw new RuntimeException(e);
										}
									}
								}
							}
						}
					}

					System.out.println(" ########### printing all new edited instructions ########### \n");

					for (InstructionHandle ih : instructionList.getInstructionHandles()) {
						Instruction inst = ih.getInstruction();
						System.out.println(inst.toString()+ "\n");
					}

					System.out.println(" ########### printing all new constants ########### \n");
					ConstantPool cp2 = cpgen.getConstantPool();
					for (int i = 0; i < cp2.getLength(); i++) {
						Constant constant = cp2.getConstant(i);
						if (constant != null) {
							System.out.println(i + ": " + constant + "\n");
						}
					}


					methodGen.setInstructionList(instructionList);
					//System.out.println("updated the instructionList\n");
					methodGen.setMaxStack(); //stack frame size needs to be correctly calculated and set
					//System.out.println("set the stack\n");
					methodGen.setMaxLocals();
					//System.out.println("set the locals\n");
					gen.replaceMethod(method, methodGen.getMethod()); //'method' = original method --> replace this with the new method with the updated InstructionList
					System.out.println("replaced the method\n");
				}else {

					System.out.println(" ########### not making any changes ########### \n");
					methodGen.setInstructionList(instructionList);
					System.out.println("updated the instructionList\n");
					methodGen.setMaxStack(); //stack frame size needs to be correctly calculated and set
					System.out.println("set the stack\n");
					methodGen.setMaxLocals();
					System.out.println("set the locals\n");
					//methodGen.removeCodeAttributes();
					//methodGen.addStackMapTable();
					//gen.replaceMethod(method, methodGen.getMethod());
					//System.out.println("replaced the method\n");
				}
			}
		}

		this.optimized = gen.getJavaClass(); //line from the original code
	}

	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}
