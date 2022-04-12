package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.Hashtable;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++){
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
	}
	
	/**	For Tasks 1 and 3: */
	descriptors = new OpenFile[16];
	boolean state = Machine.interrupt().disable();
	
	countLock = new Lock();
	countLock.acquire();
	pID = count++;
	countLock.release();
	
	descriptors[0] = UserKernel.console.openForReading();
	descriptors[1] = UserKernel.console.openForWriting();
	
	occupiedFiles[0] = true;
	currentlyOpen.put(0, descriptors[0].getName());
	fileMap.put(descriptors[0].getName(), new fileStuff(descriptors[0]));
	occupiedFiles[1] = true;
	currentlyOpen.put(1, descriptors[1].getName());
	fileMap.put(descriptors[1].getName(), new fileStuff(descriptors[1]));
	
	descriptorList = new int[16];
	
	
	
	//parentProcess = null;
	//childProcess = new LinkedList<UserProcess>();
	//childProcessStatus = new HashMap<Integer, Integer>();
	
    }
    
    
    
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, vpn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
    	
    if(pID != 0){
    	return 0;
    }

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
    
    /**
     * Handle the exit() system call. 
     */
    private int handleExit(int status) {
    	return 0;
    }
    
    /**
     * Handle the exec() system call. 
     */
    private int handleExec(int vaddr, int arg1, int arg2) {
    	return 0;
    }
    
    /**
     * Handle the join() system call. 
     */
    private int handleJoin(int pID, int status) {
    	return 0;
    }
    
    /**
     * Handle the create() system call. 
     */
    private int handleCreate(int vaddr) {
    	//Handle "null pointer address"
    	if(vaddr < 0){
			Lib.debug(dbgProcess, "handleCreate: Invalid virtual address!");
			return -1;
		}
    	//Handle fileName
		String fileName=readVirtualMemoryString(vaddr,256);
		if(fileName == null || fileName == ""){
			Lib.debug(dbgProcess, "handleCreate: Read filename failed!");
			return -1;
		}
		//Handle file Limit (16)
		int availableIndex=-1;
		for(int i=0;i<16;i++){
			if(descriptors[i]==null){
				availableIndex=i;
				break;
			}
		}
		if(availableIndex==-1){
			Lib.debug(dbgProcess, "handleCreate: File limit reached!");
			return -1;
		}
		else{
			//Handle file opening
			OpenFile file=ThreadedKernel.fileSystem.open(fileName, true);
			if(file==null){
				Lib.debug(dbgProcess, "handleCreate:Create failed!");
				return-1;
			}
			else{
				descriptors[availableIndex]=file;
				return availableIndex;
			}		
		}	
    }
    /**
     * Handle the open() system call. 
     */
    private int handleOpen(int vaddr) {
    	int ret = -1;
		String fileName = readVirtualMemoryString(vaddr, 256);
		if (fileName == null) {
			Lib.debug(dbgProcess, "handleOpen: Invalid filename address");
			return -1;
		}
		OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);
		if (file == null) {
			Lib.debug(dbgProcess, "handleOpen: File could not be created");
			return -1;
		}
		for (int i = 0; i < occupiedFiles.length; i++) {
			if (occupiedFiles[i] == false) {
				ret = i;
				break;
			}
		}
		if (ret == -1) {
			Lib.debug(dbgProcess,
					"handleCreate: There are no more free fileDescriptors");
			return ret;
		}
		if (fileMap.get(file.getName()) != null
				&& fileMap.get(file.getName()).isMarkedForDeletion() == true) {
			Lib.debug(dbgProcess, "handleOpen: file marked for deletion");
			return -1;
		}
		if (currentlyOpen.containsValue(file.getName())) {
			fileMap.get(file.getName()).incrementReferences();
			occupiedFiles[ret] = true;
			return ret;
		}
		occupiedFiles[ret] = true;
		currentlyOpen.put(ret, file.getName());
		fileMap.put(file.getName(), new fileStuff(file));
		return ret;
    }
    /**
     * Handle the read() system call. 
     */
    
  private int handleRead(int fileDescriptor, int vaddr, int size) {
		int ret = -1;
		if (fileDescriptor < 0 || fileDescriptor > 15) {
			Lib.debug(dbgProcess, "handleRead: Invalid fileDescriptor");
			return -1;
		}
		byte[] b = new byte[size];
		OpenFile readFile = fileMap.get(currentlyOpen.get(fileDescriptor)).getFile();
		if (readFile == null) {
			Lib.debug(dbgProcess, "handleRead: file not in currentlyOpen");
			return -1;
		}
		ret = readFile.read(b, 0, size);
		if (ret == -1) {
			Lib.debug(dbgProcess,
					"handleRead: readFile.read() not completed properly");
			return -1;
		}
		ret = writeVirtualMemory(vaddr, b);
		return ret;
	}

  /**
   * Handle the write() system call. 
   */
	private int handleWrite(int fileDescriptor, int vaddr, int size) {
		int ret;
		if (fileDescriptor < 0 || fileDescriptor > 15) {
			Lib.debug(dbgProcess, "handleWrite: fileDescriptor is invalid");
			return -1;
		}

		byte[] b = new byte[size];
		ret = readVirtualMemory(vaddr, b);

		OpenFile writeFile = fileMap.get(currentlyOpen.get(fileDescriptor)).getFile();
		if (writeFile == null) {
			Lib.debug(dbgProcess, "handleWrite: file is not currentlyOpen");
			return -1;
		}

		ret = writeFile.write(b, 0, size);
		if (ret == -1) {
			Lib.debug(dbgProcess,
					"handleWrite: writeFile.write() not completed properly");
			return -1;
		}

		return ret;
	}

	/**
	 * Handle the close() system call.
	 */
	private int handleClose(int fileDescriptor) {
		if (fileDescriptor < 0 || fileDescriptor > 15) {
			System.out.println("handleClose: Invalid fileDescriptor");
			Lib.debug(dbgProcess, "handleClose: Invalid fileDescriptor");
			return -1;
		}
		fileMap.get(currentlyOpen.get(fileDescriptor)).getFile().close();
		fileMap.get(currentlyOpen.get(fileDescriptor)).decrementReferences();
		currentlyOpen.remove(fileDescriptor);
		occupiedFiles[fileDescriptor] = false;
		System.out.println("Complete.");
		return 0;
	}

	/**
	 * Handle the unlink() system call.
	 */
	private int handleUnlink(int nameAddress) {
		String file = readVirtualMemoryString(nameAddress, nameLength);
		if (file == null) {
			Lib.debug(dbgProcess, "handleUnlink: filename address is invalid");

			return -1;
		}
		if (fileMap.get(file).getReferences() == 0) {
			fileMap.get(file).markForDeletion();
			fileMap.remove(file);
			ThreadedKernel.fileSystem.remove(file);
		} else {
			fileMap.get(file).markForDeletion();
		}
		return 0;
	}

    class fileStuff {
		private String file;
		private int numRef;
		private boolean markedForDelete;
		private OpenFile file1;

		private fileStuff(OpenFile inputFile) {
			file1 = inputFile;
			numRef = 1;
			markedForDelete = false;
		}

		public boolean isMarkedForDeletion() {
			return markedForDelete;
		}
		public void markForDeletion() {
			markedForDelete = true;
		}
		public OpenFile getFile() {
			return file1;
		}

		public int getReferences() {
			return numRef;
		}
		
		public void incrementReferences() {
			numRef++;
		}
	    	
	    public void decrementReferences() {
			numRef--;
		}
    }


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallExit:
		return handleExit(a0);
	case syscallExec:
		return handleExec(a0, a1, a2);
	case syscallJoin:
		return handleJoin(a0, a1);
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0, a1, a2);
	case syscallWrite:
		return handleWrite(a0, a1, a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    
    /** Added By Group 6: */
    protected OpenFile[] descriptors;	//Array of open files
    protected int[] descriptorList;
    protected int pID; //Process ID
    protected static int count = 0;
    protected Lock countLock = new Lock();
    private static final int nameLength = 256;
    
    private Hashtable<Integer, String> currentlyOpen = new Hashtable<Integer, String>();
	private static Hashtable<String, fileStuff> fileMap = new Hashtable<String, fileStuff>();
    private boolean[] occupiedFiles = new boolean[16];
    
    
    
}
