import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';

/*
  외부 Mock PG 결제 콜백 페이지
  - 팝업 창으로 열림 → postMessage로 부모(OrderCheckout)에 결과 전달 → 자동 닫힘
  - 파라미터: status, imp_uid, merchant_uid, error_msg, fail_reason
*/
const PaymentCallback = () => {
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const pgResponse = {
      status:       searchParams.get('status'),
      imp_uid:      searchParams.get('imp_uid'),
      merchant_uid: searchParams.get('merchant_uid'),
      error_msg:    searchParams.get('error_msg'),
      fail_reason:  searchParams.get('fail_reason'),
    };

    if (window.opener) {
      window.opener.postMessage(
        { type: 'PG_COMPLETE', pgResponse },
        window.location.origin
      );
      window.close();
    } else {
      // 팝업이 아닌 직접 접근 시 홈으로
      window.location.replace('/');
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="w-8 h-8 border-4 border-gray-200 border-t-gray-600 rounded-full animate-spin mx-auto mb-3" />
        <p className="text-gray-500 text-sm">결제 결과 처리 중...</p>
      </div>
    </div>
  );
};

export default PaymentCallback;
