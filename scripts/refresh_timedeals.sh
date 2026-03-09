#!/bin/bash

# EC2 정보
EC2_IP="54.206.87.196"
TEMP_KEY="/tmp/ec2-temp-key-refresh"

echo "🔑 임시 SSH 키 생성 중..."
rm -f $TEMP_KEY $TEMP_KEY.pub
ssh-keygen -t rsa -f $TEMP_KEY -N "" -q

echo "☁️ AWS EC2 Instance Connect로 키 전송 중..."
aws ec2-instance-connect send-ssh-public-key \
  --instance-id i-0c5486f6b3e201db2 \
  --instance-os-user ec2-user \
  --ssh-public-key file://$TEMP_KEY.pub \
  --profile goorm --region ap-southeast-2 > /dev/null

echo "⏳ 타임딜 날짜를 현재 시간 기준으로 연장합니다..."
ssh -o StrictHostKeyChecking=no -i $TEMP_KEY ec2-user@$EC2_IP "docker exec -i pposiraegi-db psql -U user -d ecommerce -c \"
  UPDATE time_deals 
  SET 
    start_time = NOW() - INTERVAL '30 MINUTE', 
    end_time = NOW() + INTERVAL '5 HOUR',
    status = 'ACTIVE';
\""

echo "✅ 타임딜 시간이 성공적으로 연장되었습니다! (현재시간 ~ 5시간 후)"
