import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppServer extends Thread {
    private static int PORT = 5555;
    private static String BigFile = "AppServer/data/250MB.bin";
    private static String SmallFile = "AppServer/data/100MB.bin";
    // Tamanio 0 = 100MiB y 1 = 250MiB
    private static int TamanioArchivo = 0;
    private static File logFile;

    private static int nClientes = 0;

    public static void main(String[] args) throws Exception {
        // Get current time for logFile name <año-mes-dia-hora-minuto-segundo-log.txt>
        Date now = new Date();
        String logFileName = String.format("%tY-%tm-%td-%tH-%tM-%tS-log.txt", now, now, now, now, now, now);
        logFile = new File("AppServer/logs/" + logFileName);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Ingrese el tamaño del archivo a enviar: 0 = 100MiB y 1 = 250MiB");
        TamanioArchivo = scanner.nextInt();
        System.out.println("Ingrese el puerto a utilizar: ");
        PORT = scanner.nextInt();
        System.out.println("Ingrese la cantidad de clientes a atender: ");
        nClientes = scanner.nextInt();

        File file;
        if (TamanioArchivo == 0) {
            file = new File(SmallFile);
        } else if (TamanioArchivo == 1) {
            file = new File(BigFile);
        } else {
            file = new File("AppServer/data/pexels-pixabay-206359.jpg");
        }
        System.out.println(logFile.getAbsolutePath());
        DatagramSocket socketUDP = new DatagramSocket(PORT);

        ExecutorService executor = Executors.newFixedThreadPool(nClientes);
        CyclicBarrier barrera = new CyclicBarrier(nClientes);

        //System.out.println(socketUDP.isConnected());

        for (int i = 0; i < nClientes; i++) {
            
            executor.execute(new AppServer(socketUDP, file, barrera, i));

        }

        executor.shutdown();
        
        //socketUDP.close();
        scanner.close();
    }

    public static synchronized void log(String msg) {
        /*try {
            Date now = new Date();
            FileWriter fw = new FileWriter(logFile, true);
            fw.write(now.toString() + " - " + msg + "\n");
            fw.close();
        } catch (Exception e) {
            System.err.println("Error al escribir en el log");
            e.printStackTrace();
        }*/
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
            Date now = new Date();
            bw.write(now.toString() + " - " + msg + "\n");
            bw.close();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        // Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        // Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        // Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
        ;

        // close the stream; We don't need it now.
        fis.close();

        // Get the hash's bytes
        byte[] bytes = digest.digest();

        // This bytes[] has bytes in decimal format;
        // Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        // return complete hash
        return sb.toString();
    }

    private DatagramSocket socketCliente;

    private File fileEnviar;
    private CyclicBarrier barrera;

    private int idDelegado;

    public AppServer(DatagramSocket socketUDP, File file, CyclicBarrier barrera, int id) {
        this.socketCliente = socketUDP;
        this.fileEnviar = file;
        this.barrera = barrera;
        this.idDelegado = id;
    }


    @Override
    public void run() {
        try {
            //String p1=socketCliente.getInetAddress();
            //System.out.println(socketCliente); 
            //socketCliente.connect("192.168.20.32");
            log("Server " + idDelegado + " en linea para envio del archivo " + fileEnviar.getName() + " al cliente ");
                    //+ socketCliente.getInetAddress().getHostAddress() + ":" + socketCliente.getPort());
            log("Tamanio del archivo: " + fileEnviar.length() + " bytes");
            //MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            //String checksum = getFileChecksum(md5Digest, fileEnviar);
            byte[] buffer = new byte[1024];
            
            /*DatagramPacket conexion = new DatagramPacket(buffer, buffer.length);
                 
            //Recibo el datagrama
            //System.out.println(socketCliente.isConnected());
            socketCliente.receive(conexion);
            String mensaje = new String(conexion.getData());
            System.out.println(mensaje);
            log("Server: " + idDelegado + " - Mensaje recibido " + mensaje);
            String partes[] = mensaje.split(" ");
            Integer idCliente = Integer.parseInt(partes[1]);
            mensaje = "Conexion establecida con cliente " + idCliente.toString();
            log("Server: " + idDelegado + " - " + mensaje);
            int puertoCliente = conexion.getPort();
            InetAddress direccion = conexion.getAddress();
            buffer = mensaje.getBytes();
 
                //creo el datagrama
            DatagramPacket rConexion = new DatagramPacket(buffer, buffer.length, direccion, puertoCliente);
            System.out.println("Llego");
            socketCliente.send(rConexion);
            DatagramPacket pa = new DatagramPacket(buffer, buffer.length);
            socketCliente.receive(pa);
            mensaje = new String(pa.getData());
            log("Server: " + idDelegado + " - Mensaje recibido " + mensaje);
            partes = mensaje.split(" ");
            Boolean idCorrecto = partes[1].equals(idCliente.toString());
            if (!idCorrecto) {
                mensaje = "Error en el id del cliente";
                log("Server: " + idDelegado + " - " + mensaje);
                socketCliente.close();
                return;
            }
            long tamanioEnvio = fileEnviar.length();
            mensaje = "Tamanio del archivo a enviar: " + tamanioEnvio;
            log("Server: " + idDelegado + " - " + mensaje);
            buffer = mensaje.getBytes();
            DatagramPacket rpa = new DatagramPacket(buffer, buffer.length, direccion, puertoCliente);
            socketCliente.send(rpa);*/

            // System.out.println("Checksum del archivo: " + checksum);

            /*BufferedReader in = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            PrintWriter out = new PrintWriter(socketCliente.getOutputStream(), true);*/

            // Recibir petición de envio de archivo
            
                 
            //Recibo el datagrama
            /*socketCliente.receive(peticion);
            String msg = new String(peticion.getData());
            log("Server: " + idDelegado + " - Mensaje recibido " + msg);
            String partes[] = msg.split(" ");
            Integer idCliente = Integer.parseInt(partes[1]);
            msg = "Conexion establecida con cliente " + idCliente.toString();
            log("Server: " + idDelegado + " - " + msg);
            out.println(msg);
            // Recibir petición de tamanio de archivo
            msg = in.readLine() + "";
            log("Server: " + idDelegado + " - Mensaje recibido " + msg);
            partes = msg.split(" ");
            Boolean idCorrecto = partes[1].equals(idCliente.toString());
            if (!idCorrecto) {
                msg = "Error en el id del cliente";
                log("Server: " + idDelegado + " - " + msg);
                socketCliente.close();
                return;
            }
            long tamanioEnvio = fileEnviar.length();
            msg = "Tamanio del archivo a enviar: " + tamanioEnvio;
            log("Server: " + idDelegado + " - " + msg);
            out.println(msg);*/

            /*// Recibir confirmacion de listo para recibir
            msg = in.readLine() + "";
            log("Server: " + idDelegado + " - Mensaje recibido " + msg);
            partes = msg.split(" ");
            idCorrecto = partes[1].equals(idCliente.toString());
            if (!idCorrecto) {
                msg = "Error en el id del cliente";
                log("Server: " + idDelegado + " - " + msg);
                socketCliente.close();
                return;
            }
            if (!msg.contains("listo")) {
                msg = "Error en el mensaje de listo";
                log("Server: " + idDelegado + " - " + msg);
                socketCliente.close();
                return;
            }*/

            long start = System.currentTimeMillis();

            // Enviar archivo
            Boolean enviado = false;
            String msg;
            
            DatagramPacket peticion = new DatagramPacket(buffer, buffer.length);
                 
                //Recibo el datagrama
            socketCliente.receive(peticion);
            int puertoCliente = peticion.getPort();
            InetAddress direccion = peticion.getAddress();

            while (!enviado) {
                
                // Create Streams to send the file
                //Preparo la respuesta
                FileInputStream f = new FileInputStream(fileEnviar);
                log("Server: " + idDelegado + "/" + nClientes + " - Esperando a los demas clientes");
                barrera.await();
                
                msg = "Enviando archivo...";
                log("Server: " + idDelegado + " - " + msg);
                buffer = new byte[787734];
                //int count;
                int i=0;
                /*while(f.read(buffer) != -1){

                    buffer[i]=(byte)f.read();
                    i++;
                }*/
                if (TamanioArchivo==0){
                        byte b[]=new byte[50000];
                        // ...
                        int count;
                        
                        for (int x=0;x>2000;x++)
                            while((count = f.read(b)) != -1){
                                socketCliente.send(new DatagramPacket(b,count,direccion,puertoCliente));
                            }
                        }
                if (TamanioArchivo==1){
                            byte b[]=new byte[50000];
                            // ...
                            int count;
                            
                            for (int x=0;x>5000;x++)
                                while((count = f.read(b)) != -1){
                                    socketCliente.send(new DatagramPacket(b,count,direccion,puertoCliente));
                                }
                            }
                f.close();
                //DatagramPacket message = new DatagramPacket(buffer,i,direccion, puertoCliente);
                //socketCliente.send(message);
                        
    
               /*  buffer=fileInputStream.
                DatagramPacket respuesta = new DatagramPacket(buffer, buffer.length, direccion, puertoCliente);*/


                msg = "Archivo enviado";
                log("Server: " + idDelegado + " - " + msg + " del cliente ");
                enviado=true;
                /* 
                // Recibir solicitud de hash
                msg = in.readLine();
                log("Server: " + idDelegado + " - Mensaje recibido " + msg);
                partes = msg.split(" ");
                idCorrecto = partes[1].equals(idCliente.toString());
                if (!idCorrecto) {
                    msg = "Error en el id del cliente";
                    log("Server: " + idDelegado + " - " + msg);
                    socketCliente.close();
                    fileInputStream.close();
                    return;
                }
                // Enviar hash
                msg = "Enviando checksum";
                log("Server: " + idDelegado + " - " + msg + " a cliente " + idCliente);
                out.println(checksum);
                // Recibir confirmacion de hash
                msg = in.readLine() + "";
                log("Server: " + idDelegado + " - Mensaje recibido " + msg);
                partes = msg.split(" ");
                idCorrecto = partes[1].equals(idCliente.toString());
                if (!idCorrecto) {
                    msg = "Error en el id del cliente";
                    log("Server: " + idDelegado + " - " + msg);
                    socketCliente.close();
                    fileInputStream.close();
                    return;
                }
                if (msg.contains("incorrecto")) {
                    msg = "Error en el mensaje de listo";
                    log("Server: " + idDelegado + " - " + msg);
                    enviado = false;
                } else {
                    enviado = true;
                }
                fileInputStream.close();
            }*/
        }
            long end = System.currentTimeMillis();
            log("Server: " + idDelegado + " - Tiempo de envio: " + (end - start) + " ms");

            // Terminar conexion
            msg = "Terminando conexion";
            log("Server: " + idDelegado + " - " + msg + " con cliente ");
            buffer = msg.getBytes();
 
                //creo el datagrama
            DatagramPacket respuesta = new DatagramPacket(buffer, buffer.length, direccion, puertoCliente);
            socketCliente.send(respuesta);
            socketCliente.close();

            log("Tasa de envio: " + (fileEnviar.length() / (1024*((end - start)/1000))) + " KB/s");

        } catch (Exception e) {
            e.printStackTrace();
        }
    

}
}