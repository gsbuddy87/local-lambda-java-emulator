# local-lambda-java-lemulator
> [!NOTE]
> **LocalStack Free**: This setup is compatible with LocalStack Community Edition. We use a local SQLite database in `/tmp` for storage when running locally.

> [!IMPORTANT]
> **Container Engine (Docker/Podman)**: LocalStack requires access to a container engine socket to run Lambda functions.
> 
> **If using Docker (Windows/Mac/Linux):**
> - Mount: `-v /var/run/docker.sock:/var/run/docker.sock`
> 
> **If using Podman (Windows - version 5.x):**
> 1.  **Expose Podman API**: Open a **Regular PowerShell** window and run:
>     ```powershell
>     podman system service tcp:0.0.0.0:8888 --timeout=0
>     ```
>     *Keep this window open.*
> 2.  **Restart LocalStack**:
>     ```bash
>     podman-compose down
>     podman-compose up -d
>     ```
> 3.  **Verify**: Run `curl.exe http://localhost:4567/_localstack/health` in a new terminal.
> 3. Alternatively, if on Linux/WSL2, mount `/run/user/1000/podman/podman.sock` (check your path with `podman info`).

## Prerequisites
- Java 25
- Maven
- Terraform
- LocalStack (running on `localhost:4567`)
- `awslocal` CLI (optional, but recommended for easy interaction)

## 1. Build the Project
Package the Java application into an Uber-JAR.
```bash
mvn clean package
```

## Local Verification (LocalStack)

1.  **Deploy Infrastructure**:
    ```powershell
    cd terraform
    terraform apply -var="environment=local" -auto-approve
    ```

2.  **Upload Test Files**:
    ```powershell
    # File 1
    echo "This is some test content" > file1.txt
    aws --endpoint-url=http://localhost:4567 s3 cp file1.txt s3://my-comparison-bucket-emulator/file1.txt

    # File 2 (Identical)
    aws --endpoint-url=http://localhost:4567 s3 cp file1.txt s3://my-comparison-bucket-emulator/file2.txt

    # File 3 (Different)
    echo "This is DIFFERENT content" > file3.txt
    aws --endpoint-url=http://localhost:4567 s3 cp file3.txt s3://my-comparison-bucket-emulator/file3.txt
    ```

3.  **Invoke Lambda**:
    Prepare the payload ([request.json](file:///d:/POC/git/local-lambda-java-emulator/request.json)):
    ```json
    {
      "bucketName": "my-comparison-bucket-emulator",
      "key1": "file1.txt",
      "key2": "file2.txt"
    }
    ```
    Invoke:
    ```powershell
    aws --endpoint-url=http://localhost:4567 lambda invoke --function-name s3-comparator --region us-east-1 --payload file://request.json --cli-binary-format raw-in-base64-out response.json
    type response.json
    ```

4.  **Verification Output**:
    - The `response.json` should contain: `Comparison complete. Match ID: ... Result: Content and size match.`
    - Check the SQLite results in the Lambda logs:
      ```powershell
      aws --endpoint-url=http://localhost:4567 logs tail /aws/lambda/s3-comparator --region us-east-1
      ```

## AWS Deployment
To deploy to actual AWS:
1.  Ensure you have your AWS credentials configured.
2.  Run:
    ```bash
    terraform apply -var="environment=aws"
    ```
