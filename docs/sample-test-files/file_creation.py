import random

def generate_customer_record(record_id, length=80):
    # Fields: ID (6), Name (10), City (10), Balance (8)
    cust_id = f"{record_id:06d}"
    name = f"NAME{record_id:05d}"[:10]
    city = f"CITY{random.randint(100,999)}"[:10]
    balance = f"{random.randint(1000,99999):08d}"
    
    record = cust_id + name + city + balance
    return record.ljust(length)

# File 1: Customer records
with open("customers_file1.txt", "w") as f1:
    for i in range(1, 50001):  # 50k records
        f1.write(generate_customer_record(i) + "\n")

# File 2: Exact copy of File 1
with open("customers_file2.txt", "w") as f2, open("customers_file1.txt", "r") as f1:
    f2.write(f1.read())

# File 3: Different customer records
with open("customers_file3.txt", "w") as f3:
    for i in range(50001, 100001):  # another 50k records, different IDs
        f3.write(generate_customer_record(i) + "\n")

print("Files created: customers_file1.txt, customers_file2.txt, customers_file3.txt")