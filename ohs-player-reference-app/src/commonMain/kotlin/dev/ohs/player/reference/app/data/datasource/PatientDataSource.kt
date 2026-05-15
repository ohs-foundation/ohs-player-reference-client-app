package dev.ohs.player.reference.app.data.datasource

val PATIENTS_JSON: String = """
[
  {
    "id": "p1",
    "firstName": "Amina",
    "lastName": "Diallo",
    "gender": "Female",
    "birthDate": "2015-03-14",
    "phoneNumber": "+221 77 123 4567",
    "address": { "street": "12 Rue de Dakar", "city": "Dakar", "country": "Senegal" },
    "bloodType": "O+",
    "allergies": ["Penicillin"],
    "conditions": ["Asthma"],
    "medications": [
      { "name": "Salbutamol", "dosage": "100mcg", "frequency": "As needed" }
    ],
    "emergencyContact": { "name": "Fatou Diallo", "relationship": "Mother", "phoneNumber": "+221 77 234 5678" },
    "insuranceProvider": "Senegal National Health",
    "medicalRecordNumber": "MRN-2015-0342",
    "lastVisitDate": "2026-03-10",
    "isActive": true
  },
  {
    "id": "p2",
    "firstName": "Kofi",
    "lastName": "Mensah",
    "gender": "Male",
    "birthDate": "2019-11-02",
    "phoneNumber": "+233 24 567 8901",
    "address": { "street": "5 Osu Oxford St", "city": "Accra", "country": "Ghana" },
    "bloodType": "A+",
    "allergies": [],
    "conditions": ["Sickle Cell Trait"],
    "medications": [],
    "emergencyContact": { "name": "Ama Mensah", "relationship": "Mother", "phoneNumber": "+233 24 678 9012" },
    "insuranceProvider": "NHIS Ghana",
    "medicalRecordNumber": "MRN-2019-1187",
    "lastVisitDate": "2026-02-28",
    "isActive": true
  },
  {
    "id": "p3",
    "firstName": "Jane",
    "lastName": "Okoro",
    "gender": "Female",
    "birthDate": "1992-07-21",
    "phoneNumber": "+234 803 456 7890",
    "address": { "street": "22 Allen Avenue", "city": "Lagos", "country": "Nigeria" },
    "bloodType": "B+",
    "allergies": ["Sulfonamides", "Latex"],
    "conditions": ["Hypertension", "Type 2 Diabetes"],
    "medications": [
      { "name": "Amlodipine", "dosage": "5mg", "frequency": "Once daily" },
      { "name": "Metformin", "dosage": "500mg", "frequency": "Twice daily" }
    ],
    "emergencyContact": { "name": "Chidi Okoro", "relationship": "Spouse", "phoneNumber": "+234 803 567 8901" },
    "insuranceProvider": "NHIS Nigeria",
    "medicalRecordNumber": "MRN-1992-0784",
    "lastVisitDate": "2026-04-02",
    "isActive": true
  },
  {
    "id": "p4",
    "firstName": "Samuel",
    "lastName": "Adebayo",
    "gender": "Male",
    "birthDate": "1985-01-30",
    "phoneNumber": "+234 706 789 0123",
    "address": { "street": "8 Ring Road", "city": "Ibadan", "country": "Nigeria" },
    "bloodType": "AB-",
    "allergies": [],
    "conditions": [],
    "medications": [],
    "emergencyContact": { "name": "Grace Adebayo", "relationship": "Wife", "phoneNumber": "+234 706 890 1234" },
    "insuranceProvider": "HMO Plus",
    "medicalRecordNumber": "MRN-1985-0291",
    "lastVisitDate": "2025-12-15",
    "isActive": false
  },
  {
    "id": "p5",
    "firstName": "Grace",
    "lastName": "Nakato",
    "gender": "Female",
    "birthDate": "1955-09-09",
    "phoneNumber": "+256 772 345 678",
    "address": { "street": "14 Kampala Road", "city": "Kampala", "country": "Uganda" },
    "bloodType": "O-",
    "allergies": ["Aspirin"],
    "conditions": ["Osteoarthritis", "Chronic Kidney Disease Stage 3"],
    "medications": [
      { "name": "Paracetamol", "dosage": "1g", "frequency": "Three times daily" },
      { "name": "Enalapril", "dosage": "10mg", "frequency": "Once daily" },
      { "name": "Calcium Carbonate", "dosage": "500mg", "frequency": "Twice daily" }
    ],
    "emergencyContact": { "name": "Peter Nakato", "relationship": "Son", "phoneNumber": "+256 772 456 789" },
    "insuranceProvider": "Uganda National Health Insurance",
    "medicalRecordNumber": "MRN-1955-0056",
    "lastVisitDate": "2026-04-12",
    "isActive": true
  },
  {
    "id": "p6",
    "firstName": "Daniel",
    "lastName": "Tekle",
    "gender": "Male",
    "birthDate": "1948-12-05",
    "phoneNumber": "+251 91 234 5678",
    "address": { "street": "3 Bole Medhanialem", "city": "Addis Ababa", "country": "Ethiopia" },
    "bloodType": "A-",
    "allergies": ["Codeine", "Ibuprofen"],
    "conditions": ["COPD", "Atrial Fibrillation", "Benign Prostatic Hyperplasia"],
    "medications": [
      { "name": "Tiotropium", "dosage": "18mcg", "frequency": "Once daily" },
      { "name": "Warfarin", "dosage": "5mg", "frequency": "Once daily" },
      { "name": "Tamsulosin", "dosage": "0.4mg", "frequency": "Once daily" }
    ],
    "emergencyContact": { "name": "Sara Tekle", "relationship": "Daughter", "phoneNumber": "+251 91 345 6789" },
    "insuranceProvider": "CBHI Ethiopia",
    "medicalRecordNumber": "MRN-1948-0012",
    "lastVisitDate": "2026-03-25",
    "isActive": true
  }
]
""".trimIndent()
