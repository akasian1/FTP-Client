import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;
import java.util.Scanner;

public class GestorFTP {
    private final Scanner entrada;
    private final FTPClient ftpClient;
    private static final String SERVER = "localhost";
    private static final int PORT = 21;
    private static final String USERNAME = Credenciales.USUARIO;
    private static final String PASSWORD = Credenciales.CONTRASENA;

    public GestorFTP(Scanner entrada) {
        this.ftpClient = new FTPClient();
        this.entrada = entrada;
    }


    public static void main(String[] argumentos) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        try (Scanner entrada = new Scanner(System.in, StandardCharsets.UTF_8)) {
            GestorFTP gestorFTP = new GestorFTP(entrada);
            try {
                gestorFTP.conectar();
                System.out.println("Conectado al servidor FTP.");
                gestorFTP.ejecutar();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                try {
                    gestorFTP.desconectar();
                    System.out.println("Desconectado del servidor FTP.");
                } catch (IOException e) {
                    System.err.println("Error al desconectar: " + e.getMessage());
                }
            }
        }
    }

    private void ejecutar() {
        boolean continuar = true;
        while (continuar && !Thread.currentThread().isInterrupted()) {
            System.out.println(
                    "\n1. Listar archivos y carpetas." +
                    "\n2. Subir un archivo." +
                    "\n3. Descargar un archivo." +
                    "\n4. Crear una carpeta." +
                    "\n5. Cambiar de carpeta." +
                    "\n6. Volver a la carpeta superior." +
                    "\n0. Salir."
            );
            System.out.print("Opción: ");
            if (!entrada.hasNextLine()) throw new InputMismatchException();
            String opcion = entrada.nextLine();
            if (opcion == null) break;
            try {
                switch (opcion.trim()) {
                    case "1" -> listar();
                    case "2" -> subirArchivo();
                    case "3" -> descargarArchivo();
                    case "4" -> crearCarpeta();
                    case "5" -> cambiarCarpeta();
                    case "6" -> volverPadre();
                    case "0" -> continuar = false;
                    default -> System.out.println("Opción incorrecta.");
                }
            } catch (Exception error) {
                System.out.println(error.getMessage());
                continuar = false;
            }
        }
        System.out.println("Fin del Cliente FTP.");
    }

    private void volverPadre() {
    }

    private void cambiarCarpeta() {
    }

    private boolean crearCarpeta() throws IOException {
        System.out.println("Nombre de la carpeta a crear: ");
        if (!entrada.hasNextLine()) return false;
        String nombreCarpeta = entrada.nextLine();
        if (nombreCarpeta.isEmpty()) return false;

        try {
            
            return ftpClient.makeDirectory(nombreCarpeta);
        } catch (IOException e) {
            throw new IOException("Error al subir el archivo: " + e.getMessage(), e);
        }
    }

    private void listar() throws IOException {
        System.out.println("Contenido del directorio actual :");
        FTPFile[] dirs = ftpClient.listFiles();
        if (dirs.length == 0) System.out.println("La carpeta esta vacia");
        for (int i = 0; i < dirs.length; i++) {
            if(dirs[i].isFile()) {
                System.out.println("Archivo : "+dirs[i].getName() + " de tamaño "+ dirs[i].getSize() + " Bytes");
            } else if (dirs[i].isDirectory()) {
                System.out.println("Directorio : "+dirs[i].getName());
            }
        }
    }


    public void conectar() throws SocketException, IOException {
        ftpClient.connect(SERVER, PORT);
        int replyCode = ftpClient.getReplyCode();
        //220	Servidor listo para recibir al cliente
        //230	Inicio de sesión correcto
        //530	No se ha iniciado sesión o autenticación rechazada
        //421	Servicio no disponible
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            throw new IOException("No se pudo conectar al servidor FTP, " +
                    "código de respuesta: " + replyCode);
        }
        boolean loggedIn = ftpClient.login(USERNAME, PASSWORD);
        if (!loggedIn) {
            throw new IOException("No se pudo iniciar sesión en el servidor FTP " +
                    "con las credenciales proporcionadas.");
        }
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    private void desconectar() throws IOException {
        if (ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    private boolean subirArchivo() throws IOException {
        System.out.println("Ruta del archivo local: ");
        if (!entrada.hasNextLine()) return false;
        String rutaLocal = entrada.nextLine();
        if (rutaLocal.isEmpty()) return false;
        File localFile = new File(rutaLocal);


        System.out.println("Nombre en el servidor: ");
        if (!entrada.hasNextLine()) return false;
        String rutaRemota = entrada.nextLine();
        if (rutaRemota.isEmpty()) rutaRemota = localFile.getName();

        
        
        try {

            if (!localFile.exists() || !localFile.isFile()) {
                throw new IOException("El archivo local no existe o no es un archivo válido: "
                        + rutaLocal);
            }
            try (InputStream inputStream = new FileInputStream(localFile)) {
                System.out.println("Archivo subido a "+rutaRemota);
                return ftpClient.storeFile(rutaRemota, inputStream);
            }
        } catch (IOException e) {
            throw new IOException("Error al subir el archivo: " + e.getMessage(), e);
        }
    }

    private boolean descargarArchivo() throws IOException {
        listar();
        System.out.println("Nombre en el servidor: ");
        if (!entrada.hasNextLine()) return false;
        String rutaRemota = entrada.nextLine();
        if (rutaRemota.isEmpty()) return false;

        System.out.println("Ruta del archivo local: ");
        if (!entrada.hasNextLine()) return false;
        String rutaLocal = entrada.nextLine();
        if (rutaLocal.isEmpty()) rutaLocal = rutaRemota;

        File localFile = new File(rutaLocal);
        File carpetaDestino = localFile.getAbsoluteFile().getParentFile();

        if (carpetaDestino == null || !carpetaDestino.isDirectory()) {
            throw new IOException("La carpeta de destino no existe.");
        }

        try (OutputStream outputStream = new FileOutputStream(localFile)) {
            System.out.println("Archivo bajado em "+localFile.getPath());
            return ftpClient.retrieveFile(rutaRemota, outputStream);
        } catch (IOException e) {
            throw new IOException(
                    "Error al descargar el archivo: " + e.getMessage(), e
            );
        }
    }
}


