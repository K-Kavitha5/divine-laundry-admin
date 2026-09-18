-- Reference screenshot shows INR 199 per seat. Do not modify historical order-item prices.
UPDATE laundry_services SET unit_rate = 199.00 WHERE code = 'SOFA_SEAT' AND unit_rate = 190.00;
